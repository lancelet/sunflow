package org.sunflow.core.light;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.LightSample;
import org.sunflow.core.LightSource;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Photon;
import org.sunflow.core.PhotonJ;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Point3;
import org.sunflow.math.Point3J;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class PointLight implements LightSource {
    private Point3 lightPoint;
    private Color power;

    public PointLight() {
        lightPoint = Point3J.zero();
        power = Color.WHITE;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        lightPoint = pl.getPoint("center", lightPoint);
        power = pl.getColor("power", power);
        return true;
    }

    public int getNumSamples() {
        return 1;
    }

    public void getSamples(ShadingState state) {
        Vector3 d = Point3J.sub(lightPoint, state.getPoint());
        if (d.dot(state.getNormal()) > 0 && d.dot(state.getGeoNormal()) > 0) {
            LightSample dest = new LightSample();
            // prepare shadow ray
            dest.setShadowRay(new Ray(state.getPoint(), lightPoint));
            float scale = 1.0f / (float) (4 * Math.PI * lightPoint.distanceToSquared(state.getPoint()));
            dest.setRadiance(power, power);
            dest.getDiffuseRadiance().mul(scale);
            dest.getSpecularRadiance().mul(scale);
            dest.traceShadow(state);
            state.addSample(dest);
        }
    }

    public Photon getPhoton(double randX1, double randY1, double randX2, double randY2) {
        Point3 position = lightPoint;
        
        float phi = (float) (2 * Math.PI * randX1);
        float s = (float) Math.sqrt(randY1 * (1.0f - randY1));
        Vector3 direction = Vector3J.create((float) Math.cos(phi) * s,
                                            (float) Math.sin(phi) * s,
                                            (float) (1 - 2 * randY1));
        
        Color power = new Color(this.power);

        return PhotonJ.create(position, direction, power);
    }

    public float getPower() {
        return power.getLuminance();
    }

    public Instance createInstance() {
        return null;
    }
}