package org.sunflow.core.primitive;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Instance;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Ray;
import org.sunflow.core.ShadingState;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Normal3;
import org.sunflow.math.OrthoNormalBasis;
import org.sunflow.math.Point3;
import org.sunflow.math.Solvers;
import org.sunflow.math.Vector3;
import org.sunflow.math.Vector3J;

public class Cylinder implements PrimitiveList {
    public boolean update(ParameterList pl, SunflowAPI api) {
        return true;
    }

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        BoundingBox bounds = new BoundingBox(1);
        if (o2w != null)
            bounds = o2w.transform(bounds);
        return bounds;
    }

    public float getPrimitiveBound(int primID, int i) {
        return (i & 1) == 0 ? -1 : 1;
    }

    public int getNumPrimitives() {
        return 1;
    }

    public void prepareShadingState(ShadingState state) {
        state.init();
        state.setPoint(state.getRay().getPoint());
        Instance parent = state.getInstance();
        Point3 localPoint = state.transformWorldToObject(state.getPoint());
        state.setNormal(Vector3J.normalize(Vector3J.create(localPoint.x(), localPoint.y(), 0)));

        float phi = (float) Math.atan2(state.getNormal().y(), state.getNormal().x());
        if (phi < 0)
            phi += 2 * Math.PI;
        state.getUV().x = phi / (float) (2 * Math.PI);
        state.getUV().y = (localPoint.z() + 1) * 0.5f;
        state.setShader(parent.getShader(0));
        state.setModifier(parent.getModifier(0));
        // into world space
        Normal3 worldNormal = Vector3J.normalize(state.transformNormalObjectToWorld(state.getNormal()));
        Vector3 v = state.transformVectorObjectToWorld(Vector3J.create(0, 0, 1));
        state.setNormal(worldNormal);
        state.setGeoNormal(worldNormal);
        // compute basis in world space
        state.setBasis(OrthoNormalBasis.makeFromWV(state.getNormal(), v));
    }

    public void intersectPrimitive(Ray r, int primID, IntersectionState state) {
        // intersect in local space
        float qa = r.dx * r.dx + r.dy * r.dy;
        float qb = 2 * ((r.dx * r.ox) + (r.dy * r.oy));
        float qc = ((r.ox * r.ox) + (r.oy * r.oy)) - 1;
        double[] t = Solvers.solveQuadric(qa, qb, qc);
        if (t != null) {
            // early rejection
            if (t[0] >= r.getMax() || t[1] <= r.getMin())
                return;
            if (t[0] > r.getMin()) {
                float z = r.oz + (float) t[0] * r.dz;
                if (z >= -1 && z <= 1) {
                    r.setMax((float) t[0]);
                    state.setIntersection(0);
                    return;
                }
            }
            if (t[1] < r.getMax()) {
                float z = r.oz + (float) t[1] * r.dz;
                if (z >= -1 && z <= 1) {
                    r.setMax((float) t[1]);
                    state.setIntersection(0);
                }
            }
        }
    }

    public PrimitiveList getBakingPrimitives() {
        return null;
    }
}