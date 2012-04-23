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

public class ShinyDiffuseShader implements Shader {
    private Color diff;
    private float refl;

    public ShinyDiffuseShader() {
        diff = Color.Gray();
        refl = 0.5f;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        diff = pl.getColor("diffuse", diff);
        refl = pl.getFloat("shiny", refl);
        return true;
    }

    public Color getDiffuse(ShadingState state) {
        return diff;
    }

    public Color getRadiance(ShadingState state) {
        // make sure we are on the right side of the material
        state.faceforward();
        // direct lighting
        state.initLightSamples();
        state.initCausticSamples();
        Color d = getDiffuse(state);
        Color lr = state.diffuse(d);
        if (!state.includeSpecular())
            return lr;
        float cos = state.getCosND();
        float dn = 2 * cos;
        Vector3 refDir = Vector3J.create(
                (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                (dn * state.getNormal().z()) + state.getRay().getDirection().z());
        Ray refRay = new Ray(state.getPoint(), refDir);
        // compute Fresnel term
        cos = 1 - cos;
        float cos2 = cos * cos;
        float cos5 = cos2 * cos2 * cos;

        Color ret = Color.White();
        Color r = d.$times(refl);
        ret = ret.$minus(r);
        ret = ret.$times(cos5);
        ret = ret.$plus(r);
        return lr.$plus(ret.$times(state.traceReflection(refRay, 0)));
    }

    public Color scatterPhoton(ShadingState state, Color power) {
        Color diffuse;
        // make sure we are on the right side of the material
        state.faceforward();
        diffuse = getDiffuse(state);
        state.storePhoton(state.getRay().getDirection(), power, diffuse);
        float d = diffuse.average();
        float r = d * refl;
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < d) {
            // photon is scattered
            power = power.$times(diffuse).$times(1.0f / d);
            OrthoNormalBasis onb = state.getBasis();
            double u = 2 * Math.PI * rnd / d;
            double v = state.getRandom(0, 1, 1);
            float s = (float) Math.sqrt(v);
            float s1 = (float) Math.sqrt(1.0 - v);
            Vector3 w = Vector3J.create((float) Math.cos(u) * s, (float) Math.sin(u) * s, s1);
            w = onb.transform(w);
            state.traceDiffusePhoton(new Ray(state.getPoint(), w), power);
        } else if (rnd < d + r) {
            float cos = -state.getNormal().dot(state.getRay().getDirection());
            power.$times(diffuse).$times(1.0f / d);
            // photon is reflected
            float dn = 2 * cos;
            Vector3 dir = Vector3J.create(
                    (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                    (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                    (dn * state.getNormal().z()) + state.getRay().getDirection().z());
            state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
        }
        return power;
    }
}