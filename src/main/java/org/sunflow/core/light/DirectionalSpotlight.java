package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class DirectionalSpotlight implements LightSource {
    private Point3 src;
    private Vector3 dir;
    private OrthoNormalBasis basis;
    private float r, r2;
    private Color radiance;

    public DirectionalSpotlight() {
        src = new Point3(0, 0, 0);
        dir = Vector3J.create(0, 0, -1);
        basis = OrthoNormalBasis.makeFromW(dir);
        r = 1;
        r2 = r * r;
        radiance = Color.WHITE;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        src = pl.getPoint("source", src);
        dir = Vector3J.normalize(pl.getVector("dir", dir));
        r = pl.getFloat("radius", r);
        basis = OrthoNormalBasis.makeFromW(dir);
        r2 = r * r;
        radiance = pl.getColor("radiance", radiance);
        return true;
    }

    public int getNumSamples() {
        return 1;
    }

    public int getLowSamples() {
        return 1;
    }

    public void getSamples(ShadingState state) {
        if (dir.dot(state.getGeoNormal()) < 0 && dir.dot(state.getNormal()) < 0) {
            // project point onto source plane
            float x = state.getPoint().x - src.x;
            float y = state.getPoint().y - src.y;
            float z = state.getPoint().z - src.z;
            float t = ((x * dir.x()) + (y * dir.y()) + (z * dir.z()));
            if (t >= 0.0) {
                x -= (t * dir.x());
                y -= (t * dir.y());
                z -= (t * dir.z());
                if (((x * x) + (y * y) + (z * z)) <= r2) {
                    Point3 p = new Point3();
                    p.x = src.x + x;
                    p.y = src.y + y;
                    p.z = src.z + z;
                    LightSample dest = new LightSample();
                    dest.setShadowRay(new Ray(state.getPoint(), p));
                    dest.setRadiance(radiance, radiance);
                    dest.traceShadow(state);
                    state.addSample(dest);
                }
            }
        }
    }

    public Vector3 getPhoton(double randX1, double randY1, double randX2, double randY2, Point3 p, Color power) {
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(1.0f - randY1);
        Vector3 ldir = Vector3J.create(r * (float) Math.cos(phi) * s,
                                       r * (float) Math.sin(phi) * s,
                                       0);
        ldir = basis.transform(ldir);
        Point3.add(src, ldir, p);
        power.set(radiance).mul((float) Math.PI * r2);
        return dir;
    }

    public float getPower() {
        return radiance.copy().mul((float) Math.PI * r2).getLuminance();
    }

    public Instance createInstance() {
        return null;
    }
}