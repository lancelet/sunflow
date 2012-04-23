package org.sunflow.core.gi;

import org.sunflow.core.GIEngine;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

/**
 * This is a quick way to get a bit of ambient lighting into your scene with
 * hardly any overhead. It's based on the formula found here:
 * 
 * @link http://www.cs.utah.edu/~shirley/papers/rtrt/node7.html#SECTION00031100000000000000
 */
public class FakeGIEngine implements GIEngine {
    private Vector3 up;
    private Color sky;
    private Color ground;

    public Color getIrradiance(ShadingState state, Color diffuseReflectance) {
        float cosTheta = up.dot(state.getNormal());
        float sin2 = (1 - cosTheta * cosTheta);
        float sine = sin2 > 0 ? (float) Math.sqrt(sin2) * 0.5f : 0;
        if (cosTheta > 0)
            return sky.lerpTo(ground, sine);
        else
            return ground.lerpTo(sky, sine);
    }

    public Color getGlobalRadiance(ShadingState state) {
        return Color.Black();
    }

    public boolean init(Options options, Scene scene) {
        up = Vector3J.normalize(options.getVector(
                "gi.fake.up", Vector3J.create(0, 1, 0)));
        sky = options.getColor("gi.fake.sky", Color.White());
        ground = options.getColor("gi.fake.ground", Color.Black());
        sky = sky.$times((float) Math.PI);
        ground = ground.$times((float) Math.PI);
        return true;
    }
}