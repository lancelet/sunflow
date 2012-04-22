package org.sunflow.math

trait Point3 {
  def x: Float
  def y: Float
  def z: Float
  
  def +(v: Vector3)(implicit b: Point3Builder): Point3 =
    b(x + v.x, y + v.y, z + v.z)
  def -(v: Vector3)(implicit b: Point3Builder): Point3 =
    b(x - v.x, y - v.y, z - v.z)
  def -(p: Point3)(implicit b: Vector3Builder): Vector3 =
    b(x - p.x, y - p.y, z - p.z)
  def mid(p: Point3)(implicit b: Point3Builder): Point3 =
    this + (p - this) * 0.5f
  def lerpTo(p: Point3, pAmount: Float)(implicit b: Point3Builder): Point3 =
    this + (p - this) * pAmount
  
  def distanceTo(p: Point3): Float = (this - p).length
  def distanceToSquared(p: Point3): Float = (this - p).lengthSquared
}

trait Point3Builder extends ((Float, Float, Float) => Point3)

object Point3 {
  private implicit val normal3Builder: Normal3Builder = 
    Normal3.SimpleNormal3Builder
  def apply(x: Float, y: Float, z: Float)(implicit b: Point3Builder): Point3 =
    b(x, y, z)
  def normal(p0: Point3, p1: Point3, p2: Point3): Normal3 =
    ((p1 - p0) cross (p2 - p0)).normalize
  private [Point3] final case class SimplePoint3(
      x: Float, y: Float, z: Float) extends Point3
  private [Point3] sealed class SimplePoint3Builder extends Point3Builder {
    def apply(x: Float, y: Float, z: Float): Point3 = SimplePoint3(x, y, z)
  }
  implicit object SimplePoint3Builder extends SimplePoint3Builder
  val Zero = SimplePoint3(0, 0, 0)
}

// Static call interface for Java.
object Point3J {
  def create(x: Float, y: Float, z: Float): Point3 = Point3(x, y, z)
  def add(p: Point3, v: Vector3): Point3 = p + v
  def sub(p: Point3, v: Vector3): Point3 = p - v
  def sub(p0: Point3, p1: Point3): Vector3 = p0 - p1
  def mid(p0: Point3, p1: Point3): Point3 = p0 mid p1
  def lerpTo(p0: Point3, p1: Point3, p1Amount: Float): Point3 = 
    p0.lerpTo(p1, p1Amount)
  def normal(p0: Point3, p1: Point3, p2: Point3): Normal3 =
    Point3.normal(p0, p1, p2)
  def zero(): Point3 = Point3.Zero
}
