package org.sunflow.core

import org.sunflow.image.Color
import org.sunflow.math.{Matrix4, MovingMatrix4, Point2, Point3, Vector3}
import scala.collection.immutable.IndexedSeq

/** Parameter.
  * 
  * Parameters may appear as any one of several types (for example: Strings,
  * Ints, Booleans, etc.).  However, by default, a parameter has only one
  * type.
  * 
  * To construct a Parameter, use one of the two constructor forms:
  *   Parameter(value)
  *   Parameter(value, interpolation)
  * These constructors will select either a ScalarParameterBuilder (in the
  * first case), or a ParameterBuilder (second case) type class to perform
  * the actual Parameter construction.
  * 
  * Parameters should typically be stored and accessed from a parameter map.
  * For example:
  *   val pmap: Map[String, Parameter[_]]
  *   pmap("my_cool_parameter").getFloat.getOrElse(4.0f)
  */
trait Parameter[T] {
  def value: T
  def interpolation: Parameter.Interpolation = Parameter.NoInterpolation
  
  // scalar parameter access
  def getString: Option[String] = None
  def getInt: Option[Int] = None
  def getBoolean: Option[Boolean] = None
  def getFloat: Option[Float] = None
  def getPoint3: Option[Point3] = None
  def getVector3: Option[Vector3] = None
  def getTexCoord: Option[Point2] = None
  def getMatrix4: Option[Matrix4] = None
  def getColor: Option[Color] = None
  def getMovingMatrix: Option[MovingMatrix4] = None
  
  // indexed sequence parameter access
  def getFloatIndexedSeq: Option[IndexedSeq[Float]] = None
  def getIntIndexedSeq: Option[IndexedSeq[Int]] = None
  def getMatrix4IndexedSeq: Option[IndexedSeq[Matrix4]] = None
  def getPoint3IndexedSeq: Option[IndexedSeq[Point3]] = None
  def getStringIndexedSeq: Option[IndexedSeq[String]] = None
  def getTexCoordIndexedSeq: Option[IndexedSeq[Point2]] = None
  def getVector3IndexedSeq: Option[IndexedSeq[Vector3]] = None
}

object Parameter {

  def apply[T](value: T)
  (implicit m: Manifest[T], b: ScalarParameterBuilder[T]): Parameter[T] =
    b.build(value)
    
  def apply[T](value: T, interpolation: Interpolation)
  (implicit m: Manifest[T], b: ParameterBuilder[T]): Parameter[T] =
    b.build(value, interpolation)
  
  // ScalarParameterBuilder and ParameterBuilder are both type classes so that
  //  other parameters may implicitly implement their own.
  trait ScalarParameterBuilder[T] {
    def build(value: T): Parameter[T]
  }
  trait ParameterBuilder[T] {
    def build(value: T, interpolation: Interpolation): Parameter[T]
  }
  private type SPB[T] = ScalarParameterBuilder[T]
  private type PB[T] = ParameterBuilder[T]
    
  sealed case class Interpolation(name: String)
  object NoInterpolation extends Interpolation("NoInterpolation")
  object Face extends Interpolation("Face")
  object Vertex extends Interpolation("Vertex")
  object FaceVarying extends Interpolation("FaceVarying")
  
  implicit object StringScalarParameterBuilder extends ASB[String]
  implicit object IntScalarParameterBuilder extends ASB[Int]
  implicit object BooleanScalarParameterBuilder extends ASB[Boolean]
  implicit object FloatScalarParameterBuilder extends ASB[Float]
  implicit object Point3ScalarParameterBuilder extends ASB[Point3]
  implicit object Vector3ScalarParameterBuilder extends ASB[Vector3]
  implicit object TexCoordScalarParameterBuilder extends ASB[Point2]
  implicit object Matrix4ScalarParameterBuilder extends ASB[Matrix4]
  implicit object ColorScalarParameterBuilder extends ASB[Color]
  implicit object MovMatrix4ScalarParamBuilder extends ASB[MovingMatrix4]

  sealed abstract class ASB[T : Manifest] extends SPB[T] {
    def build(value: T): Parameter[T] = AutoScalarParameter(value)
  }
  
  sealed abstract class AP[T : Manifest] extends PB[T] {
    def build(value: IndexedSeq[T], interpolation: Interpolation): Parameter[T]
  }
  
  private final case class AutoScalarParameter[T : Manifest](value: T) 
  extends Parameter[T] {
    private val valueManifest: Manifest[T] = implicitly[Manifest[T]]
    override val interpolation: Interpolation = NoInterpolation
    
    private def as[Q : Manifest]: Option[Q] = {
      if (valueManifest <:< implicitly[Manifest[Q]]) {
        Some(value.asInstanceOf[Q])
      } else {
        None
      }
    }
    
    override def getString: Option[String] = as[String]
    override def getInt: Option[Int] = as[Int]
    override def getBoolean: Option[Boolean] = as[Boolean]
    override def getFloat: Option[Float] = as[Float]
    override def getPoint3: Option[Point3] = as[Point3]
    override def getVector3: Option[Vector3] = as[Vector3]
    override def getTexCoord: Option[Point2] = as[Point2]
    override def getMatrix4: Option[Matrix4] = as[Matrix4]
    override def getColor: Option[Color] = as[Color]
    override def getMovingMatrix: Option[MovingMatrix4] = as[MovingMatrix4]
  }
  
  private final case class AutoParameter[T : Manifest]
    (value: IndexedSeq[T], 
     override val interpolation: Interpolation = NoInterpolation)
  extends Parameter[IndexedSeq[T]]
  {
    private val valueManifest: Manifest[IndexedSeq[T]] = 
      implicitly[Manifest[IndexedSeq[T]]]
    
    private def as[Q : Manifest]: Option[IndexedSeq[Q]] = {
      // use the Manifest to ensure a safe cast
      if (valueManifest <:< implicitly[Manifest[IndexedSeq[Q]]]) {
        Some(value.asInstanceOf[IndexedSeq[Q]])
      } else {
        None
      }
    }

    override def getFloatIndexedSeq: Option[IndexedSeq[Float]] = as[Float]
    override def getIntIndexedSeq: Option[IndexedSeq[Int]] = as[Int]
    override def getMatrix4IndexedSeq: Option[IndexedSeq[Matrix4]] = 
      as[Matrix4]
    override def getPoint3IndexedSeq: Option[IndexedSeq[Point3]] = as[Point3]
    override def getStringIndexedSeq: Option[IndexedSeq[String]] = as[String]
    override def getTexCoordIndexedSeq: Option[IndexedSeq[Point2]] = as[Point2]
    override def getVector3IndexedSeq: Option[IndexedSeq[Vector3]] = 
      as[Vector3]

  }
  
}
