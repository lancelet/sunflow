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

public class PhongShader implements Shader {
    private Color diff;
    private Color spec;
    private float power;
    private int numRays;

    public PhongShader() {
        diff = Color.Gray();
        spec = Color.Gray();
        power = 20;
        numRays = 4;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        diff = pl.getColor("diffuse", diff);
        spec = pl.getColor("specular", spec);
        power = pl.getFloat("power", power);
        numRays = pl.getInt("samples", numRays);
        return true;
    }

    protected Color getDiffuse(ShadingState state) {
        return diff;
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        // setup lighting
        state.initLightSamples();
        state.initCausticSamples();
        // execute shader
        return state.diffuse(getDiffuse(state)).$plus(state.specularPhong(spec, power, numRays));
    }

    public Color scatterPhoton(ShadingState state, Color power) {
        // make sure we are on the right side of the material
        state.faceforward();
        Color d = getDiffuse(state);
        float avgD = d.average();
        float avgS = spec.average();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < avgD) {
            // photon is scattered diffusely
            power = power.$times(d).$times(1.0f / avgD);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / avgD;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0f - v);
            Vector3 w = Vector3J.create((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w);
            state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        } else if (rnd < avgD + avgS) {
            /* lancelet: this does not seem to be used
            // photon is scattered specularly
            float dn = 2.0f * state.getCosND();
            // reflected direction
            Vector3 refDir = new Vector3(
                    (dn * state.getNormal().x()) + state.getRay().dx,
                    (dn * state.getNormal().y()) + state.getRay().dy,
                    (dn * state.getNormal().z()) + state.getRay().dz);
            */
            power = power.$times(spec).$times(1.0f / avgS);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * (rnd - avgD) / avgS;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.pow(v, 1 / (this.power + 1));
            float s1 = (float) Math.sqrt(1 - s * s);
            Vector3 w = Vector3J.create((float) Math.cos(u) * s1, (float) Math.sin(u) * s1, s);
            w = onb.transform(w);
            state.traceReflectionPhoton(new Ray(state.getPoint(), w), power);
        }
        state.storePhoton(state.getRay().getDirection(), power, d);
        return power;
    }
}