package org.sunflow.math;

/**
 * 3D axis-aligned bounding box. Stores only the minimum and maximum corner
 * points.
 */
public class BoundingBox {
    private Point3 minimum;
    private Point3 maximum;

    /**
     * Creates an empty box. The minimum point will have all components set to
     * positive infinity, and the maximum will have all components set to
     * negative infinity.
     */
    public BoundingBox() {
        minimum = Point3J.create(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        maximum = Point3J.create(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }
    
    /**
     * Creates a box from a set of 6 explicit minimum and maximum coordinates.
     * @param minx minimum x value
     * @param miny minimum y value
     * @param minz minimum z value
     * @param maxx maximum x value
     * @param maxy maximum y value
     * @param maxz maximum z value
     */
    public BoundingBox(float minx, float miny, float minz, 
                       float maxx, float maxy, float maxz) 
    {
        if ((minx > maxx) || (miny > maxy) || (minz > maxz)) {
            throw new IllegalArgumentException(
               "Some of the bounding box values were not ordered correctly."
            );
        }
        minimum = Point3J.create(minx, miny, minz);
        maximum = Point3J.create(maxx, maxy, maxz);
    }

    /**
     * Creates a copy of the given box.
     * 
     * @param b bounding box to copy
     */
    public BoundingBox(BoundingBox b) {
        minimum = b.minimum;
        maximum = b.maximum;
    }

    /**
     * Creates a bounding box containing only the specified point.
     * 
     * @param p point to include
     */
    public BoundingBox(Point3 p) {
        this(p.x(), p.y(), p.z());
    }

    /**
     * Creates a bounding box containing only the specified point.
     * 
     * @param x x coordinate of the point to include
     * @param y y coordinate of the point to include
     * @param z z coordinate of the point to include
     */
    public BoundingBox(float x, float y, float z) {
        minimum = Point3J.create(x, y, z);
        maximum = Point3J.create(x, y, z);
    }

    /**
     * Creates a bounding box centered around the origin.
     * 
     * @param size half edge length of the bounding box
     */
    public BoundingBox(float size) {
        minimum = Point3J.create(-size, -size, -size);
        maximum = Point3J.create(size, size, size);
    }

    /**
     * Gets the minimum corner of the box. That is the corner of smallest
     * coordinates on each axis. Note that the returned reference is not cloned
     * for efficiency purposes so care must be taken not to change the
     * coordinates of the point.
     * 
     * @return a reference to the minimum corner
     */
    public final Point3 getMinimum() {
        return minimum;
    }

    /**
     * Gets the maximum corner of the box. That is the corner of largest
     * coordinates on each axis. Note that the returned reference is not cloned
     * for efficiency purposes so care must be taken not to change the
     * coordinates of the point.
     * 
     * @return a reference to the maximum corner
     */
    public final Point3 getMaximum() {
        return maximum;
    }

    /**
     * Gets the center of the box, computed as (min + max) / 2.
     * 
     * @return a reference to the center of the box
     */
    public final Point3 getCenter() {
        return Point3J.mid(minimum, maximum);
    }

    /**
     * Gets a corner of the bounding box. The index scheme uses the binary
     * representation of the index to decide which corner to return. Corner 0 is
     * equivalent to the minimum and corner 7 is equivalent to the maximum.
     * 
     * @param i a corner index, from 0 to 7
     * @return the corresponding corner
     */
    public final Point3 getCorner(int i) {
        float x = (i & 1) == 0 ? minimum.x() : maximum.x();
        float y = (i & 2) == 0 ? minimum.y() : maximum.y();
        float z = (i & 4) == 0 ? minimum.z() : maximum.z();
        return Point3J.create(x, y, z);
    }

    /**
     * Gets a specific coordinate of the surface's bounding box.
     * 
     * @param i index of a side from 0 to 5
     * @return value of the request bounding box side
     */
    public final float getBound(int i) {
        switch (i) {
            case 0:
                return minimum.x();
            case 1:
                return maximum.x();
            case 2:
                return minimum.y();
            case 3:
                return maximum.y();
            case 4:
                return minimum.z();
            case 5:
                return maximum.z();
            default:
                return 0;
        }
    }

    /**
     * Gets the extents vector for the box. This vector is computed as (max -
     * min). Its coordinates are always positive and represent the dimensions of
     * the box along the three axes.
     * 
     * @return a refreence to the extent vector
     * @see org.sunflow.math.Vector3#length()
     */
    public final Vector3 getExtents() { return Point3J.sub(maximum, minimum); }

    /**
     * Gets the surface area of the box.
     * 
     * @return surface area
     */
    public final float getArea() {
        Vector3 w = getExtents();
        float ax = Math.max(w.x(), 0);
        float ay = Math.max(w.y(), 0);
        float az = Math.max(w.z(), 0);
        return 2 * (ax * ay + ay * az + az * ax);
    }

    /**
     * Gets the box's volume
     * 
     * @return volume
     */
    public final float getVolume() {
        Vector3 w = getExtents();
        float ax = Math.max(w.x(), 0);
        float ay = Math.max(w.y(), 0);
        float az = Math.max(w.z(), 0);
        return ax * ay * az;
    }

    /**
     * Enlarge the bounding box by the minimum possible amount to avoid numeric
     * precision related problems.
     */
    public final void enlargeUlps() {
        final float eps = 0.0001f;
        minimum = Point3J.sub(minimum, Vector3J.create(
                Math.max(eps, Math.ulp(minimum.x())),
                Math.max(eps, Math.ulp(minimum.y())),
                Math.max(eps, Math.ulp(minimum.z()))));
        maximum = Point3J.add(maximum, Vector3J.create(
                Math.max(eps, Math.ulp(maximum.x())),
                Math.max(eps, Math.ulp(maximum.y())),
                Math.max(eps, Math.ulp(maximum.z()))));
    }

    /**
     * Returns <code>true</code> when the box has just been initialized, and
     * is still empty. This method might also return true if the state of the
     * box becomes inconsistent and some component of the minimum corner is
     * larger than the corresponding coordinate of the maximum corner.
     * 
     * @return <code>true</code> if the box is empty, <code>false</code>
     *         otherwise
     */
    public final boolean isEmpty() {
        return (maximum.x() < minimum.x()) || 
               (maximum.y() < minimum.y()) || 
               (maximum.z() < minimum.z());
    }

    /**
     * Returns <code>true</code> if the specified bounding box intersects this
     * one. The boxes are treated as volumes, so a box inside another will
     * return true. Returns <code>false</code> if the parameter is
     * <code>null</code>.
     * 
     * @param b box to be tested for intersection
     * @return <code>true</code> if the boxes overlap, <code>false</code>
     *         otherwise
     */
    public final boolean intersects(BoundingBox b) {
        return ((b != null) && (minimum.x() <= b.maximum.x()) && 
                               (maximum.x() >= b.minimum.x()) && 
                               (minimum.y() <= b.maximum.y()) && 
                               (maximum.y() >= b.minimum.y()) && 
                               (minimum.z() <= b.maximum.z()) && 
                               (maximum.z() >= b.minimum.z()));
    }

    /**
     * Checks to see if the specified {@link org.sunflow.math.Point3 point}is
     * inside the volume defined by this box. Returns <code>false</code> if
     * the parameter is <code>null</code>.
     * 
     * @param p point to be tested for containment
     * @return <code>true</code> if the point is inside the box,
     *         <code>false</code> otherwise
     */
    public final boolean contains(Point3 p) {
        return ((p != null) && (p.x() >= minimum.x()) && 
                               (p.x() <= maximum.x()) && 
                               (p.y() >= minimum.y()) && 
                               (p.y() <= maximum.y()) && 
                               (p.z() >= minimum.z()) && 
                               (p.z() <= maximum.z()));
    }

    /**
     * Check to see if the specified point is inside the volume defined by this
     * box.
     * 
     * @param x x coordinate of the point to be tested
     * @param y y coordinate of the point to be tested
     * @param z z coordinate of the point to be tested
     * @return <code>true</code> if the point is inside the box,
     *         <code>false</code> otherwise
     */
    public final boolean contains(float x, float y, float z) {
        return ((x >= minimum.x()) && (x <= maximum.x()) && 
                (y >= minimum.y()) && (y <= maximum.y()) && 
                (z >= minimum.z()) && (z <= maximum.z()));
    }

    /**
     * Changes the extents of the box as needed to include the given
     * {@link org.sunflow.math.Point3 point}into this box. Does nothing if the
     * parameter is <code>null</code>.
     * 
     * @param p point to be included
     */
    public final void include(Point3 p) {
        float minx = minimum.x();
        float miny = minimum.y();
        float minz = minimum.z();
        float maxx = maximum.x();
        float maxy = maximum.y();
        float maxz = maximum.z();
        if (p != null) {
            if (p.x() < minx)
                minx = p.x();
            if (p.x() > maxx)
                maxx = p.x();
            if (p.y() < miny)
                miny = p.y();
            if (p.y() > maxy)
                maxy = p.y();
            if (p.z() < minz)
                minz = p.z();
            if (p.z() > maxz)
                maxz = p.z();
        }
        minimum = Point3J.create(minx, miny, minz);
        maximum = Point3J.create(maxx, maxy, maxz);
    }

    /**
     * Changes the extents of the box as needed to include the given point into
     * this box.
     * 
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     * @param z z coordinate of the point
     */
    public final void include(float x, float y, float z) {
        include(Point3J.create(x, y, z));
    }

    /**
     * Changes the extents of the box as needed to include the given box into
     * this box. Does nothing if the parameter is <code>null</code>.
     * 
     * @param b box to be included
     */
    public final void include(BoundingBox b) {
        float minx = minimum.x();
        float miny = minimum.y();
        float minz = minimum.z();
        float maxx = maximum.x();
        float maxy = maximum.y();
        float maxz = maximum.z();
        if (b != null) {
            if (b.minimum.x() < minx)
                minx = b.minimum.x();
            if (b.maximum.x() > maxx)
                maxx = b.maximum.x();
            if (b.minimum.y() < miny)
                miny = b.minimum.y();
            if (b.maximum.y() > maxy)
                maxy = b.maximum.y();
            if (b.minimum.z() < minz)
                minz = b.minimum.z();
            if (b.maximum.z() > maxz)
                maxz = b.maximum.z();
        }
        minimum = Point3J.create(minx, miny, minz);
        maximum = Point3J.create(maxx, maxy, maxz);
    }

    @Override
    public final String toString() {
        return String.format("(%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)", 
                minimum.x(), minimum.y(), minimum.z(), 
                maximum.x(), maximum.y(), maximum.z());
    }
}
