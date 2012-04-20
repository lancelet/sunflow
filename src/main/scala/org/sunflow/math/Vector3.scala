package org.sunflow.math

import scala.math.sqrt

trait Vector3 {
  def x: Float
  def y: Float
  def z: Float
  
  def lengthSquared: Float = (x * x) + (y * y) + (z * z)
  def length: Float = sqrt(lengthSquared).toFloat
    
  def unary_-()(implicit b: Vector3Builder): Vector3 = 
    b(-x, -y, -z) 
  def +(v: Vector3)(implicit b: Vector3Builder): Vector3 =
    b(x + v.x, y + v.y, z + v.z)
  def -(v: Vector3)(implicit b: Vector3Builder): Vector3 =
    b(x - v.x, y - v.y, z - v.z)
  def *(s: Float)(implicit b: Vector3Builder): Vector3 = 
    b(x * s, y * s, z * s) 
  def /(d: Float)(implicit b: Vector3Builder): Vector3 =
    b(x / d, y / d, z / d)
  def normalize()(implicit b: Normal3Builder): Normal3 = {
    val l: Float = length
    /* TODO: lancelet: sometimes zero-length vectors are fed in.
    assert(l > 0, "attempted to normalize a zero-length vector")
    */
    b(x / l, y / l, z / l)
  }
  def dot(v: Vector3): Float =
    (x * v.x) + (y * v.y) + (z * v.z)
  def cross(v: Vector3)(implicit b: Vector3Builder): Vector3 =
    b((y * v.z) - (z * v.y),
      (z * v.x) - (x * v.z),
      (x * v.y) - (y * v.x))
}

trait Vector3Builder extends ((Float, Float, Float) => Vector3)

object Vector3 {
  def apply(x: Float, y: Float, z: Float)(implicit b: Vector3Builder): Vector3 =
    b(x, y, z)
  private [Vector3] final case class SimpleVector3(
      x: Float, y: Float, z: Float) extends Vector3 {
    override lazy val lengthSquared: Float = super.lengthSquared
    override lazy val length: Float = super.length
  }
  private [Vector3] sealed class SimpleVector3Builder extends Vector3Builder {
    def apply(x: Float, y: Float, z: Float): Vector3 = SimpleVector3(x, y, z)
  }
  implicit object SimpleVector3Builder extends SimpleVector3Builder
  val Zero = SimpleVector3(0, 0, 0)
}

// Static call interface for Java.
object Vector3J {
  def create(x: Float, y: Float, z: Float): Vector3 = Vector3(x, y, z)
  def negate(v: Vector3): Vector3 = -v
  def normalize(v: Vector3): Normal3 = v.normalize();
  def divide(v: Vector3, l: Float): Vector3 = v / l
  def cross(v1: Vector3, v2: Vector3): Vector3 = v1 cross v2
  def add(v1: Vector3, v2: Vector3): Vector3 = v1 + v2
  def zero(): Vector3 = Vector3.Zero
}
