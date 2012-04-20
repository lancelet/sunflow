package org.sunflow.math;

public final class OrthoNormalBasis {
    private Vector3 u, v, w;

    private OrthoNormalBasis() {
        u = Vector3J.zero();
        v = Vector3J.zero();
        w = Vector3J.zero();
    }

    public void flipU() {
        u = Vector3J.negate(u);
    }

    public void flipV() {
        v = Vector3J.negate(v);
    }

    public void flipW() {
        w = Vector3J.negate(w);
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
        return Vector3J.create(x, y, z);
    }

    public Vector3 untransform(Vector3 a) {
        return Vector3J.create(a.dot(u), a.dot(v), a.dot(w));
    }

    public float untransformX(Vector3 a) { return a.dot(u); }

    public float untransformY(Vector3 a) { return a.dot(v); }

    public float untransformZ(Vector3 a) { return a.dot(w); }

    public static final OrthoNormalBasis makeFromW(Vector3 w) {
        OrthoNormalBasis onb = new OrthoNormalBasis();
        onb.w = Vector3J.normalize(w);
        if ((Math.abs(onb.w.x()) < Math.abs(onb.w.y())) && (Math.abs(onb.w.x()) < Math.abs(onb.w.z()))) {
            onb.v = Vector3J.normalize(Vector3J.create(0, onb.w.z(), -onb.w.y()));
        } else if (Math.abs(onb.w.y()) < Math.abs(onb.w.z())) {
            onb.v = Vector3J.normalize(Vector3J.create(onb.w.z(), 0, -onb.w.x()));
        } else {
            onb.v = Vector3J.normalize(Vector3J.create(onb.w.y(), -onb.w.x(), 0));
        }
        onb.u = Vector3J.cross(onb.v, onb.w);
        return onb;
    }

    public static final OrthoNormalBasis makeFromWV(Vector3 w, Vector3 v) {
        OrthoNormalBasis onb = new OrthoNormalBasis();
        onb.w = Vector3J.normalize(w);
        Vector3 c = Vector3J.cross(v, onb.w);
        /* TODO: lancelet: sometimes the u, v vectors are colinear
        if (c.length() <= 1.0e-6) {
            // TODO: Warning
            return makeFromW(onb.w);
        }
        */
        onb.u = Vector3J.normalize(c);
        onb.v = Vector3J.cross(onb.w, onb.u);
        return onb;
    }
}