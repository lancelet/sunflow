package org.sunflow.math

trait Point2 {
  def x: Float
  def y: Float
}

trait Point2Builder extends ((Float, Float) => Point2)

object Point2 {
  def apply(x: Float, y: Float)(implicit b: Point2Builder): Point2 = b(x, y)
  private [Point2] final case class SimplePoint2(x: Float, y: Float) extends
      Point2
  private [Point2] sealed class SimplePoint2Builder extends Point2Builder {
    def apply(x: Float, y: Float): Point2 = SimplePoint2(x, y)
  }
  implicit object SimplePoint2Builder extends SimplePoint2Builder
  val Zero = SimplePoint2(0, 0)
}

// Static call interface for Java.
object Point2J {
  def create(x: Float, y: Float): Point2 = Point2(x, y)
  def zero(): Point2 = Point2.Zero
}