package org.sunflow.math;

public final class OrthoNormalBasis {
    private Vector3 u, v, w;

    private OrthoNormalBasis() {
        u = new Vector3(0, 0, 0);
        v = new Vector3(0, 0, 0);
        w = new Vector3(0, 0, 0);
    }

    public void flipU() {
        u = u.unary_$minus();
    }

    public void flipV() {
        v = v.unary_$minus();
    }

    public void flipW() {
        w = w.unary_$minus();
    }

    public void swapUV() {
        Vector3 t = u;
        u = v;
        v = t;
    }

    public void swapVW() {
        Vector3 t = v;
        v = w;
        w = t;
    }

    public void swapWU() {
        Vector3 t = w;
        w = u;
        u = t;
    }


    public Vector3 transform(Vector3 a) {
        float x = (a.x() * u.x()) + (a.y() * v.x()) + (a.z() * w.x());
        float y = (a.x() * u.y()) + (a.y() * v.y()) + (a.z() * w.y());
        float z = (a.x() * u.z()) + (a.y() * v.z()) + (a.z() * w.z());
        return new Vector3(x, y, z);
    }

    public Vector3 untransform(Vector3 a) {
        return new Vector3(a.dot(u), a.dot(v), a.dot(w));
    }

    public float untransformX(Vector3 a) { return a.dot(u); }

    public float untransformY(Vector3 a) { return a.dot(v); }

    public float untransformZ(Vector3 a) { return a.dot(w); }

    public static final OrthoNormalBasis makeFromW(Vector3 w) {
        OrthoNormalBasis onb = new OrthoNormalBasis();
        onb.w = w.normalize();
        if ((Math.abs(onb.w.x()) < Math.abs(onb.w.y())) && (Math.abs(onb.w.x()) < Math.abs(onb.w.z()))) {
            onb.v = new Vector3(0, onb.w.z(), -onb.w.y()).normalize();
        } else if (Math.abs(onb.w.y()) < Math.abs(onb.w.z())) {
            onb.v = new Vector3(onb.w.z(), 0, -onb.w.x()).normalize();
        } else {
            onb.v = new Vector3(onb.w.y(), -onb.w.x(), 0).normalize();
        }
        onb.u = onb.v.cross(onb.w);
        return onb;
    }

    public static final OrthoNormalBasis makeFromWV(Vector3 w, Vector3 v) {
        OrthoNormalBasis onb = new OrthoNormalBasis();
        onb.w = w.normalize();
        onb.u = v.cross(onb.w).normalize();
        onb.v = onb.w.cross(onb.u);
        return onb;
    }
}