package org.sunflow.math;

public final class Vector3J {
    private static final float[] COS_THETA = new float[256];
    private static final float[] SIN_THETA = new float[256];
    private static final float[] COS_PHI = new float[256];
    private static final float[] SIN_PHI = new float[256];

    static {
        // precompute tables to compress unit vectors
        for (int i = 0; i < 256; i++) {
            double angle = (i * Math.PI) / 256.0;
            COS_THETA[i] = (float) Math.cos(angle);
            SIN_THETA[i] = (float) Math.sin(angle);
            COS_PHI[i] = (float) Math.cos(2 * angle);
            SIN_PHI[i] = (float) Math.sin(2 * angle);
        }
    }

    private Vector3J() {}
    
    public static final Vector3 decode(short n) {
        int t = (n & 0xFF00) >>> 8;
        int p = n & 0xFF;
        float x = SIN_THETA[t] * COS_PHI[p];
        float y = SIN_THETA[t] * SIN_PHI[p];
        float z = COS_THETA[t];
        return new Vector3(x, y, z);
    }

    public static final short encode(Vector3 v) {
        int theta = (int) (Math.acos(v.z()) * (256.0 / Math.PI));
        if (theta > 255)
            theta = 255;
        int phi = (int) (Math.atan2(v.y(), v.x()) * (128.0 / Math.PI));
        if (phi < 0)
            phi += 256;
        else if (phi > 255)
            phi = 255;
        return (short) (((theta & 0xFF) << 8) | (phi & 0xFF));
    }
}
