package org.sunflow.core.shader;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.Shader;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

public class UVShader implements Shader {
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public Color getRadiance(ShadingState state) {
        if (state.getUV() == null)
            return Color.Black();
        return new Color(state.getUV().x(), state.getUV().y(), 0);
    }

    public Color scatterPhoton(ShadingState state, Color power) {
        return power;
    }
}