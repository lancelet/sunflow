package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class MirrorShader implements Shader {
    private Color color;

    public MirrorShader() {
        color = Color.White();
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        color = pl.getColor("color", color);
        return true;
    }

    public Color getRadiance(ShadingState state) {
        if (!state.includeSpecular())
            return Color.Black();
        state.faceforward();
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
        ret = ret.$minus(color);
        ret = ret.$times(cos5);
        ret = ret.$plus(color);
        return ret.$times(state.traceReflection(refRay, 0));
    }

    public Color scatterPhoton(ShadingState state, Color power) {
        float avg = color.average();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd >= avg)
            return power;
        state.faceforward();
        float cos = state.getCosND();
        power = power.$times(color).$times(1.0f / avg);
        // photon is reflected
        float dn = 2 * cos;
        Vector3 dir = Vector3J.create(
                (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                (dn * state.getNormal().z()) + state.getRay().getDirection().z());
        state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
        return power;
    }
}