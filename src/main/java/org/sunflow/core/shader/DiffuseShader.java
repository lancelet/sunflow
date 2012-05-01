package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class DiffuseShader implements Shader {
    private Color diff;

    public DiffuseShader() {
        diff = Color.White();
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        diff = pl.getColor("diffuse", diff);
        return true;
    }

    public Color getDiffuse(ShadingState state) {
        return diff;
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        // setup lighting
        state.initLightSamples();
        state.initCausticSamples();
        return state.diffuse(getDiffuse(state));
    }

    public Color scatterPhoton(ShadingState state, Color power) {
        // make sure we are on the right side of the material
        if (state.getNormal().dot(state.getRay().getDirection()) > 0.0) {
            state.setNormal(Vector3J.normalize(Vector3J.negate(state.getNormal())));
            state.setGeoNormal(Vector3J.normalize(Vector3J.negate(state.getGeoNormal())));
        }
        Color diffuse = getDiffuse(state);
        float avg = diffuse.average();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < avg) {
            // photon is scattered
            power = power.$times(diffuse).$times(1.0f / avg);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / avg;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0 - v);
            Vector3 w = Vector3J.create((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w);
            power = state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        }
        state.storePhoton(state.getRay().getDirection(), power, diffuse);
        return power;
    }
}