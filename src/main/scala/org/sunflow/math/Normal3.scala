package org.sunflow.math

trait Normal3 extends Vector3 {
  val x: Float
  val y: Float
  val z: Float
  
  import Normal3._
  
  // check that actual normal length is 1.0f
  /* TODO: lancelet: the geometry engine sometimes feeds in NaNs
  assert ({
    val l2: Float = (x * x) + (y * y) + (z * z)
    ((l2 + AllowedDeviation >= 1.0f) && (l2 - AllowedDeviation <= 1.0f))
  }, "Triplet (%f, %f, %f) was not approximately normal." format (x, y, z))
  */
  
  def unary_-()(b: Normal3Builder): Normal3 = b(-x, -y, -z)
  
  override def lengthSquared: Float = 1.0f
  override def length: Float = 1.0f
  override def normalize()(implicit b: Normal3Builder): Normal3 = this
}

trait Normal3Builder extends ((Float, Float, Float) => Normal3)

object Normal3 {
  private [Normal3] val AllowedDeviation: Float = 1e-6f;
  private [Normal3] final case class SimpleNormal3(
      x: Float, y: Float, z: Float) extends Normal3
  private [Normal3] sealed class SimpleNormal3Builder extends Normal3Builder {
    def apply(x: Float, y: Float, z: Float): Normal3 = SimpleNormal3(x, y, z)
  }
  implicit object SimpleNormal3Builder extends SimpleNormal3Builder
}
