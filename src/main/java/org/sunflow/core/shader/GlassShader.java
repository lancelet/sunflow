package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Ray;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;

public class GlassShader implements Shader {
    private float eta; // refraction index ratio
    private float f0; // fresnel normal incidence
    private Color color;
    private float absorptionDistance;
    private Color absorptionColor;

    public GlassShader() {
        eta = 1.3f;
        color = Color.WHITE;
        absorptionDistance = 0; // disabled by default
        absorptionColor = Color.GRAY; // 50% absorbtion
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        color = pl.getColor("color", color);
        eta = pl.getFloat("eta", eta);
        f0 = (1 - eta) / (1 + eta);
        f0 = f0 * f0;
        absorptionDistance = pl.getFloat("absorption.distance", absorptionDistance);
        absorptionColor = pl.getColor("absorption.color", absorptionColor);
        return true;
    }

    public Color getRadiance(ShadingState state) {
        if (!state.includeSpecular())
            return Color.BLACK;
        state.faceforward();
        float cos = state.getCosND();
        boolean inside = state.isBehind();
        float neta = inside ? eta : 1.0f / eta;

        float dn = 2 * cos;
        Vector3 reflDir = new Vector3(
                (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                (dn * state.getNormal().z()) + state.getRay().getDirection().z());

        // refracted ray
        float rlx, rly, rlz;
        float arg = 1 - (neta * neta * (1 - (cos * cos)));
        boolean tir = arg < 0;
        if (tir)
            rlx = rly = rlz = 0;
        else {
            float nK = (neta * cos) - (float) Math.sqrt(arg);
            rlx = (neta * state.getRay().dx) + (nK * state.getNormal().x());
            rly = (neta * state.getRay().dy) + (nK * state.getNormal().y());
            rlz = (neta * state.getRay().dz) + (nK * state.getNormal().z());
        }
        Vector3 refrDir = new Vector3(rlx, rly, rlz);        

        // compute Fresnel terms
        float cosTheta1 = state.getNormal().dot(reflDir);
        float cosTheta2 = -state.getNormal().dot(refrDir);

        float pPara = (cosTheta1 - eta * cosTheta2) / (cosTheta1 + eta * cosTheta2);
        float pPerp = (eta * cosTheta1 - cosTheta2) / (eta * cosTheta1 + cosTheta2);
        float kr = 0.5f * (pPara * pPara + pPerp * pPerp);
        float kt = 1 - kr;

        Color absorbtion = null;
        if (inside && absorptionDistance > 0) {
            // this ray is inside the object and leaving it
            // compute attenuation that occured along the ray
            absorbtion = Color.mul(-state.getRay().getMax() / absorptionDistance, absorptionColor.copy().opposite()).exp();
            if (absorbtion.isBlack())
                return Color.BLACK; // nothing goes through
        }
        // refracted ray
        Color ret = Color.black();
        if (!tir) {
            ret.madd(kt, state.traceRefraction(new Ray(state.getPoint(), refrDir), 0)).mul(color);
        }
        if (!inside || tir)
            ret.add(Color.mul(kr, state.traceReflection(new Ray(state.getPoint(), reflDir), 0)).mul(color));
        return absorbtion != null ? ret.mul(absorbtion) : ret;
    }

    public void scatterPhoton(ShadingState state, Color power) {
        Color refr = Color.mul(1 - f0, color);
        Color refl = Color.mul(f0, color);
        float avgR = refl.getAverage();
        float avgT = refr.getAverage();
        double rnd = state.getRandom(0, 0, 1);
        if (rnd < avgR) {
            state.faceforward();
            // don't reflect internally
            if (state.isBehind())
                return;
            // photon is reflected
            float cos = state.getCosND();
            power.mul(refl).mul(1.0f / avgR);
            float dn = 2 * cos;
            Vector3 dir = new Vector3(
                    (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                    (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                    (dn * state.getNormal().z()) + state.getRay().getDirection().z());
            state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
        } else if (rnd < avgR + avgT) {
            state.faceforward();
            // photon is refracted
            float cos = state.getCosND();
            float neta = state.isBehind() ? eta : 1.0f / eta;
            power.mul(refr).mul(1.0f / avgT);
            float wK = -neta;
            float arg = 1 - (neta * neta * (1 - (cos * cos)));
            if (state.isBehind() && absorptionDistance > 0) {
                // this ray is inside the object and leaving it
                // compute attenuation that occured along the ray
                power.mul(Color.mul(-state.getRay().getMax() / absorptionDistance, absorptionColor.copy().opposite()).exp());
            }
            if (arg < 0) {
                // TIR
                float dn = 2 * cos;
                Vector3 dir = new Vector3(
                        (dn * state.getNormal().x()) + state.getRay().getDirection().x(),
                        (dn * state.getNormal().y()) + state.getRay().getDirection().y(),
                        (dn * state.getNormal().z()) + state.getRay().getDirection().z());
                state.traceReflectionPhoton(new Ray(state.getPoint(), dir), power);
            } else {
                float nK = (neta * cos) - (float) Math.sqrt(arg);
                Vector3 dir = new Vector3(
                        (-wK * state.getRay().dx) + (nK * state.getNormal().x()),
                        (-wK * state.getRay().dy) + (nK * state.getNormal().y()),
                        (-wK * state.getRay().dz) + (nK * state.getNormal().z()));
                state.traceRefractionPhoton(new Ray(state.getPoint(), dir), power);
            }
        }
    }
}