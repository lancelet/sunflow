package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.Ray;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class AmbientOcclusionGIEngine implements GIEngine {
    private Color bright;
    private Color dark;
    private int samples;
    private float maxDist;

    public Color getGlobalRadiance(ShadingState state) {
        return Color.Black();
    }

    public boolean init(Options options, Scene scene) {
        bright = options.getColor("gi.ambocc.bright", Color.White());
        dark = options.getColor("gi.ambocc.dark", Color.Black());
        samples = options.getInt("gi.ambocc.samples", 32);
        maxDist = options.getFloat("gi.ambocc.maxdist", 0);
        maxDist = (maxDist <= 0) ? Float.POSITIVE_INFINITY : maxDist;
        return true;
    }

    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        OrthoNormalBasis onb = state.getBasis();
        Color result = Color.Black();
        for (int i = 0; i < samples; i++) {
            float xi = (float) state.getRandom(i, 0, samples);
            float xj = (float) state.getRandom(i, 1, samples);
            float phi = (float) (2 * Math.PI * xi);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            float sinTheta = (float) Math.sqrt(xj);
            float cosTheta = (float) Math.sqrt(1.0f - xj);
            Vector3 w = Vector3J.create(cosPhi * sinTheta,
                                        sinPhi * sinTheta,
                                        cosTheta);
            onb.transform(w);
            Ray r = new Ray(state.getPoint(), w);
            r.setMax(maxDist);
            result = result.$plus(bright.lerpTo(dark, state.traceShadow(r)));
        }
        return result.$times((float) Math.PI / samples);
    }
}