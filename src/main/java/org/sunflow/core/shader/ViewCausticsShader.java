package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.LightSample;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class ViewCausticsShader implements Shader {
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        state.faceforward();
        state.initCausticSamples();
        // integrate a diffuse function
        Color lr = Color.Black();
        for (LightSample sample : state) {
            Color ldiff = sample.getDiffuseRadiance();
            lr = lr.$plus(ldiff.$times(sample.dot(state.getNormal())));
        }
        return lr.$times(1.0f / (float) Math.PI);

    }

    public Color scatterPhoton(ShadingState state, Color power) {
        return power;
    }
}