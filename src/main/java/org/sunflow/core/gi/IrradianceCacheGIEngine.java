package org.sunflow.core.gi;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sunflow.PluginRegistry;
import org.sunflow.core.GIEngine;
import org.sunflow.core.GlobalPhotonMapInterface;
import org.sunflow.core.Options;
import org.sunflow.core.Ray;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.MathUtils;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Point3J;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class IrradianceCacheGIEngine implements GIEngine {
    private int samples;
    private float tolerance;
    private float invTolerance;
    private float minSpacing;
    private float maxSpacing;
    private Node root;
    private ReentrantReadWriteLock rwl;
    private GlobalPhotonMapInterface globalPhotonMap;

    public boolean init(Options options, Scene scene) {
        // get settings
        samples = options.getInt("gi.irr-cache.samples", 256);
        tolerance = options.getFloat("gi.irr-cache.tolerance", 0.05f);
        invTolerance = 1.0f / tolerance;
        minSpacing = options.getFloat("gi.irr-cache.min_spacing", 0.05f);
        maxSpacing = options.getFloat("gi.irr-cache.max_spacing", 5.00f);
        root = null;
        rwl = new ReentrantReadWriteLock();
        globalPhotonMap = PluginRegistry.globalPhotonMapPlugins.createObject(options.getString("gi.irr-cache.gmap", null));
        // check settings
        samples = Math.max(0, samples);
        minSpacing = Math.max(0.001f, minSpacing);
        maxSpacing = Math.max(0.001f, maxSpacing);
        // display settings
        UI.printInfo(Module.LIGHT, "Irradiance cache settings:");
        UI.printInfo(Module.LIGHT, "  * Samples: %d", samples);
        if (tolerance <= 0)
            UI.printInfo(Module.LIGHT, "  * Tolerance: off");
        else
            UI.printInfo(Module.LIGHT, "  * Tolerance: %.3f", tolerance);
        UI.printInfo(Module.LIGHT, "  * Spacing: %.3f to %.3f", minSpacing, maxSpacing);
        // prepare root node
        Vector3 ext = scene.getBounds().getExtents();
        root = new Node(scene.getBounds().getCenter(), 1.0001f * MathUtils.max(ext.x(), ext.y(), ext.z()));
        // init global photon map
        return (globalPhotonMap != null) ? scene.calculatePhotons(globalPhotonMap, "global", 0, options) : true;
    }

    public Color getGlobalRadiance(ShadingState state) {
        if (globalPhotonMap == null) {
            if (state.getShader() != null)
                return state.getShader().getRadiance(state);
            else
                return Color.Black();
        } else
            return globalPhotonMap.getRadiance(state.getPoint(), state.getNormal());
    }

    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        if (samples <= 0)
            return Color.Black();
        if (state.getDiffuseDepth() > 0) {
            // do simple path tracing for additional bounces (single ray)
            float xi = (float) state.getRandom(0, 0, 1);
            float xj = (float) state.getRandom(0, 1, 1);
            float phi = (float) (xi * 2 * Math.PI);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            Vector3 w = Vector3J.create(cosPhi * sinTheta,
                                        sinPhi * sinTheta,
                                        cosTheta);
            OrthoNormalBasis onb = state.getBasis();
            w = onb.transform(w);
            Ray r = new Ray(state.getPoint(), w);
            ShadingState temp = state.traceFinalGather(r, 0);
            return temp != null ? getGlobalRadiance(temp).$times((float) Math.PI) : Color.Black();
        }
        rwl.readLock().lock();
        Color irr = getIrradiance(state.getPoint(), state.getNormal());
        rwl.readLock().unlock();
        if (irr == null) {
            // compute new sample
            irr = Color.Black();
            OrthoNormalBasis onb = state.getBasis();
            float invR = 0;
            float minR = Float.POSITIVE_INFINITY;
            for (int i = 0; i < samples; i++) {
                float xi = (float) state.getRandom(i, 0, samples);
                float xj = (float) state.getRandom(i, 1, samples);
                float phi = (float) (xi * 2 * Math.PI);
                float cosPhi = (float) Math.cos(phi);
                float sinPhi = (float) Math.sin(phi);
                float sinTheta = (float) Math.sqrt(xj);
                float cosTheta = (float) Math.sqrt(1.0f - xj);
                Vector3 w = Vector3J.create(cosPhi * sinTheta,
                                            sinPhi * sinTheta,
                                            cosTheta);
                w = onb.transform(w);
                Ray r = new Ray(state.getPoint(), w);
                ShadingState temp = state.traceFinalGather(r, i);
                if (temp != null) {
                    minR = Math.min(r.getMax(), minR);
                    invR += 1.0f / r.getMax();
                    temp.getInstance().prepareShadingState(temp);
                    irr = irr.$plus(getGlobalRadiance(temp));
                }
            }
            irr = irr.$times((float) Math.PI / samples);
            invR = samples / invR;
            rwl.writeLock().lock();
            insert(state.getPoint(), state.getNormal(), invR, irr);
            rwl.writeLock().unlock();
            // view irr-cache points
            // irr = Color.YELLOW.copy().mul(1e6f);
        }
        return irr;
    }

    private void insert(Point3 p, Vector3 n, float r0, Color irr) {
        if (tolerance <= 0)
            return;
        Node node = root;
        r0 = MathUtils.clamp(r0 * tolerance, minSpacing, maxSpacing) * invTolerance;
        if (root.isInside(p)) {
            while (node.sideLength >= (4.0 * r0 * tolerance)) {
                int k = 0;
                k |= (p.x() > node.center.x()) ? 1 : 0;
                k |= (p.y() > node.center.y()) ? 2 : 0;
                k |= (p.z() > node.center.z()) ? 4 : 0;
                if (node.children[k] == null) {
                    float dcx = ((k & 1) == 0) ? -node.quadSideLength : node.quadSideLength;
                    float dcy = ((k & 2) == 0) ? -node.quadSideLength : node.quadSideLength;
                    float dcz = ((k & 4) == 0) ? -node.quadSideLength : node.quadSideLength;
                    Vector3 dCenter = Vector3J.create(dcx, dcy, dcz);
                    Point3 c = Point3J.add(node.center, dCenter);
                    node.children[k] = new Node(c, node.halfSideLength);
                }
                node = node.children[k];
            }
        }
        Sample s = new Sample(p, n, r0, irr);
        s.next = node.first;
        node.first = s;
    }

    private Color getIrradiance(Point3 p, Vector3 n) {
        if (tolerance <= 0)
            return null;
        Sample x = new Sample(p, n);
        float w = root.find(x);
        return (x.irr == null) ? null : x.irr.$times(1.0f / w);
    }

    private final class Node {
        Node[] children;
        Sample first;
        Point3 center;
        float sideLength;
        float halfSideLength;
        float quadSideLength;

        Node(Point3 center, float sideLength) {
            children = new Node[8];
            for (int i = 0; i < 8; i++)
                children[i] = null;
            this.center = center;
            this.sideLength = sideLength;
            halfSideLength = 0.5f * sideLength;
            quadSideLength = 0.5f * halfSideLength;
            first = null;
        }

        final boolean isInside(Point3 p) {
            return (Math.abs(p.x() - center.x()) < halfSideLength) && 
                   (Math.abs(p.y() - center.y()) < halfSideLength) && 
                   (Math.abs(p.z() - center.z()) < halfSideLength);
        }

        final float find(Sample x) {
            float weight = 0;
            for (Sample s = first; s != null; s = s.next) {
                float c2 = 1.0f - (x.nix * s.nix + x.niy * s.niy + x.niz * s.niz);
                float d2 = (x.pix - s.pix) * (x.pix - s.pix) + (x.piy - s.piy) * (x.piy - s.piy) + (x.piz - s.piz) * (x.piz - s.piz);
                if (c2 > tolerance * tolerance || d2 > maxSpacing * maxSpacing)
                    continue;
                float invWi = (float) (Math.sqrt(d2) * s.invR0 + Math.sqrt(Math.max(c2, 0)));
                if (invWi < tolerance || d2 < minSpacing * minSpacing) {
                    float wi = Math.min(1e10f, 1.0f / invWi);
                    if (x.irr != null)
                        x.irr = x.irr.$plus(s.irr.$times(wi));
                    else
                        x.irr = s.irr.$times(wi);
                    weight += wi;
                }
            }
            for (int i = 0; i < 8; i++)
                if ((children[i] != null) && 
                    (Math.abs(children[i].center.x() - x.pix) <= halfSideLength) && 
                    (Math.abs(children[i].center.y() - x.piy) <= halfSideLength) && 
                    (Math.abs(children[i].center.z() - x.piz) <= halfSideLength))
                {
                    weight += children[i].find(x);
                }
            return weight;
        }
    }

    private static final class Sample {
        float pix, piy, piz;
        float nix, niy, niz;
        float invR0;
        Color irr;
        Sample next;

        Sample(Point3 p, Vector3 n) {
            pix = p.x();
            piy = p.y();
            piz = p.z();
            Vector3 ni = Vector3J.normalize(n);
            nix = ni.x();
            niy = ni.y();
            niz = ni.z();
            irr = null;
            next = null;
        }

        Sample(Point3 p, Vector3 n, float r0, Color irr) {
            pix = p.x();
            piy = p.y();
            piz = p.z();
            Vector3 ni = Vector3J.normalize(n);
            nix = ni.x();
            niy = ni.y();
            niz = ni.z();
            invR0 = 1.0f / r0;
            this.irr = irr;
            next = null;
        }
    }
}