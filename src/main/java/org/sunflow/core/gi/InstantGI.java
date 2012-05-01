package org.sunflow.core.gi;

import java.util.ArrayList;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.PhotonStore;
import org.sunflow.core.Ray;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class InstantGI implements GIEngine {
    private int numPhotons;
    private int numSets;
    private float c;
    private int numBias;
    private PointLight[][] virtualLights;

    public Color getGlobalRadiance(ShadingState state) {
        Point3 p = state.getPoint();
        Vector3 n = state.getNormal();
        int set = (int) (state.getRandom(0, 1, 1) * numSets);
        float maxAvgPow = 0;
        float minDist = 1;
        Color pow = null;
        for (PointLight vpl : virtualLights[set]) {
            maxAvgPow = Math.max(maxAvgPow, vpl.power.average());
            if (n.dot(vpl.n) > 0.9f) {
                float d = vpl.p.distanceToSquared(p);
                if (d < minDist) {
                    pow = vpl.power;
                    minDist = d;
                }
            }
        }
        return pow == null ? Color.Black() : pow.$times(1.0f / maxAvgPow);
    }

    public boolean init(Options options, Scene scene) {
        numPhotons = options.getInt("gi.igi.samples", 64);
        numSets = options.getInt("gi.igi.sets", 1);
        c = options.getFloat("gi.igi.c", 0.00003f);
        numBias = options.getInt("gi.igi.bias_samples", 0);
        virtualLights = null;
        if (numSets < 1)
            numSets = 1;
        UI.printInfo(Module.LIGHT, "Instant Global Illumination settings:");
        UI.printInfo(Module.LIGHT, "  * Samples:     %d", numPhotons);
        UI.printInfo(Module.LIGHT, "  * Sets:        %d", numSets);
        UI.printInfo(Module.LIGHT, "  * Bias bound:  %f", c);
        UI.printInfo(Module.LIGHT, "  * Bias rays:   %d", numBias);
        virtualLights = new PointLight[numSets][];
        if (numPhotons > 0) {
            for (int i = 0, seed = 0; i < virtualLights.length; i++, seed += numPhotons) {
                PointLightStore map = new PointLightStore();
                if (!scene.calculatePhotons(map, "virtual", seed, options))
                    return false;
                virtualLights[i] = map.virtualLights.toArray(new PointLight[map.virtualLights.size()]);
                UI.printInfo(Module.LIGHT, "Stored %d virtual point lights for set %d of %d", virtualLights[i].length, i + 1, numSets);
            }
        } else {
            // create an empty array
            for (int i = 0; i < virtualLights.length; i++)
                virtualLights[i] = new PointLight[0];
        }
        return true;
    }

    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        float b = (float) Math.PI * c / diffuseReflectance.max();
        Color irr = Color.Black();
        Point3 p = state.getPoint();
        Vector3 n = state.getNormal();
        int set = (int) (state.getRandom(0, 1, 1) * numSets);
        for (PointLight vpl : virtualLights[set]) {
            Ray r = new Ray(p, vpl.p);
            float dotNlD = -r.dot(vpl.n);
            float dotND = r.dot(n);
            if (dotNlD > 0 && dotND > 0) {
                float r2 = r.getMax() * r.getMax();
                Color opacity = state.traceShadow(r);
                Color power = vpl.power.lerpTo(Color.Black(), opacity);
                float g = (dotND * dotNlD) / r2;
                irr = irr.$plus(power.$times(0.25f * Math.min(g, b)));
            }
        }
        // bias compensation
        int nb = (state.getDiffuseDepth() == 0 || numBias <= 0) ? numBias : 1;
        if (nb <= 0) {
            return irr;
        }
        OrthoNormalBasis onb = state.getBasis();
        float scale = (float) Math.PI / nb;
        for (int i = 0; i < nb; i++) {
            float xi = (float) state.getRandom(i, 0, nb);
            float xj = (float) state.getRandom(i, 1, nb);
            float phi = (float) (xi * 2 * Math.PI);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            Vector3 w = Vector3J.create(cosPhi * sinTheta,
                                        sinPhi * sinTheta,
                                        cosTheta);
            onb.transform(w);
            Ray r = new Ray(state.getPoint(), w);
            r.setMax((float) Math.sqrt(cosTheta / b));
            ShadingState temp = state.traceFinalGather(r, i);
            if (temp != null) {
                temp.getInstance().prepareShadingState(temp);
                if (temp.getShader() != null) {
                    float dist = temp.getRay().getMax();
                    float r2 = dist * dist;
                    float cosThetaY = -w.dot(temp.getNormal());
                    if (cosThetaY > 0) {
                        float g = (cosTheta * cosThetaY) / r2;
                        // was this path accounted for yet?
                        if (g > b) {
                            Color rad = temp.getShader().getRadiance(temp);
                            irr = irr.$plus(rad.$times(scale * (g - b) / g));
                        }
                    }
                }
            }
        }
        return irr;
    }

    private static final class PointLight {
        final Point3 p;
        final Vector3 n;
        final Color power;
        public PointLight(Point3 p, Vector3 n, Color power) {
            this.p = p;
            this.n = n;
            this.power = power;
        }
    }

    private final class PointLightStore implements PhotonStore {
        final ArrayList<PointLight> virtualLights = new ArrayList<PointLight>();

        public int numEmit() {
            return numPhotons;
        }

        public void prepare(Options options, BoundingBox sceneBounds) {
        }

        public void store(ShadingState state, Vector3 dir, Color power, Color diffuse) {
            state.faceforward();
            PointLight vpl = new PointLight(state.getPoint(), 
                                            state.getNormal(),
                                            power);
            synchronized (this) {
                virtualLights.add(vpl);
            }
        }

        public void init() {
        }

        public boolean allowDiffuseBounced() {
            return true;
        }

        public boolean allowReflectionBounced() {
            return true;
        }

        public boolean allowRefractionBounced() {
            return true;
        }
    }
}