package org.sunflow.image

import scala.math.{ exp => mexp, abs }
import scala.collection.immutable.Seq
import org.sunflow.math.MathUtils.{ clamp => mclamp }

// TODO: Remove private constructor when we use this from Scala
final case class Color private[image] (r: Float, g: Float, b: Float) {
  
  import Color._
  
  lazy val toNonLinear: Color = Color(NativeSpace.gammaCorrect(r),
                                      NativeSpace.gammaCorrect(g),
                                      NativeSpace.gammaCorrect(b))
  lazy val toLinear: Color = Color(NativeSpace.ungammaCorrect(r),
                                   NativeSpace.ungammaCorrect(g),
                                   NativeSpace.ungammaCorrect(b))
                                   
  def isBlack: Boolean = (r <= 0) && (g <= 0) && (b <= 0)
  def luminance: Float = (0.2989f * r) + (0.5866f * g) + (0.1145f * b)
  def min: Float = ((r min g) min b)
  def max: Float = ((r max g) max b)
  def average: Float = (r + g + b) / 3.0f
  def toArray: Array[Float] = Array(r, g, b)
  def toIntRGB: Int = {
    val ir: Int = mclamp((r * 255 + 0.5).toInt, 0, 255)
    val ig: Int = mclamp((g * 255 + 0.5).toInt, 0, 255)
    val ib: Int = mclamp((b * 255 + 0.5).toInt, 0, 255)
    (ir << 16) | (ig << 8) | ib
  }
  def toIntRGBA(a: Float): Int = {
    val ia: Int = mclamp((a * 255 + 0.5).toInt, 0, 255)
    ia << 24 | toIntRGB
  }
  
  /** Encode the color into 32 bits while preserving HDR using Ward's RGBE
    * technique. */
  def toIntRGBE: Int = {
    if (max < 1e-32f) {
      0
    } else {
      var m = max
      var e = 0
      if (max > 1.0f) {
        while (m > 1.0f) {
          m = m * 0.5f
          e = e + 1
        }
      } else if (max <= 0.5f) {
        while (m <= 0.5f) {
          m = m * 2.0f
          e = e - 1
        }
      }
      m = (m * 255.0f) / max
      var c = e + 128
      c = c | ((r * m).toInt << 24)
      c = c | ((g * m).toInt << 16)
      c = c | ((b * m).toInt << 8)
      c
    }
  }
  
  def constrainRGB: Color = {
    val w = (((0.0f min r) min g) min b)
    if (w > 0) Color(r + w, g + w, b + w) else this
  }
  
  def isNaN: Boolean = r.isNaN || g.isNaN || b.isNaN
  def isInf: Boolean = r.isInfinity || g.isInfinity || b.isInfinity
  
  def +(c: Color): Color = Color(r + c.r, g + c.g, b + c.b)
  def -(c: Color): Color = Color(r - c.r, g - c.g, b - c.b)
  def *(f: Float): Color = Color(r * f, g * f, b * f)
  def *(c: Color): Color = Color(r * c.r, g * c.g, b * c.b)
  def /(f: Float): Color = Color(r / f, g / f, b / f)
  def exp: Color = Color(mexp(r).toFloat, mexp(g).toFloat, mexp(b).toFloat)
  def opposite: Color = Color(1 - r, 1 - g, 1 - b)
  def clamp(min: Float, max: Float) =
    Color(mclamp(r, min, max), mclamp(g, min, max), mclamp(b, min, max))
  def lerpTo(c: Color, cAmount: Float): Color = this + (c - this) * cAmount
  def lerpTo(c: Color, cAmount: Color): Color =
    Color(r * (1 - cAmount.r) + c.r * cAmount.r, 
          g * (1 - cAmount.g) + c.g * cAmount.g, 
          b * (1 - cAmount.b) + c.b * cAmount.b)
  def hasContrast(c: Color, thresh: Float): Boolean = {
    (abs(r - c.r) / (r + c.r) > thresh) ||
    (abs(g - c.g) / (g + c.g) > thresh) ||
    (abs(b - c.b) / (b + c.b) > thresh)
  }
}


object Color {
  val NativeSpace = RGBSpace.SRGB
  val Black   = Color(0, 0, 0)
  val White   = Color(1, 1, 1)
  val Red     = Color(1, 0, 0)
  val Green   = Color(0, 1, 0)
  val Blue    = Color(0, 0, 1)
  val Yellow  = Color(1, 1, 0)
  val Cyan    = Color(0, 1, 1)
  val Magenta = Color(1, 0, 1)
  val Gray    = Color(0.5f, 0.5f, 0.5f)
  
  def fromGray(g: Float) = Color(g, g, g)
  def fromIntRGB(rgb: Int): Color = {
    val r = ((rgb >> 16) & 0xFF) / 255.0f
    val g = ((rgb >> 8) & 0xFF) / 255.0f
    val b = (rgb & 0xFF) / 255.0f
    Color(r, g, b)
  }
  
  private [Color] val Exponent: Array[Float] = {
    val exponent = new Array[Float](256)
    for (i <- 0 until 256) {
      var f = 1.0f;
      val e = i - (128 + 8)
      if (e > 0) {
        for (j <- 0 until e) f = f * 2.0f
      } else {
        for (j <- 0 until -e) f = f * 0.5f
      }
      exponent(i) = f
    }
    exponent
  }
  def fromIntRGBE(rgbe: Int): Color = {
    val f = Exponent(rgbe & 0xFF)
    val r = f * ((rgbe >>> 24) + 0.5f)
    val g = f * (((rgbe >> 16) & 0xFF) + 0.5f)
    val b = f * (((rgbe >> 8) & 0xFF) + 0.5f)
    Color(r, g, b)
  }
}

object ColorJ {
  import Color._
  def create(r: Float, g: Float, b: Float): Color = Color(r, g, b)
}
