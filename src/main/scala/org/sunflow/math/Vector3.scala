package org.sunflow.math

import scala.math.sqrt

case class Vector3(x: Float, y: Float, z: Float) {
  lazy val lengthSquared: Float = (x * x) + (y * y) + (z * z)
  lazy val length: Float = sqrt(lengthSquared).toFloat
  def unary_-() = Vector3(-x, -y, -z)
  def *(s: Float) = Vector3(x * s, y * s, z * s)
  def /(d: Float) = Vector3(x / d, y / d, z / d)
  def normalize() = Vector3(x / length, y / length, z / length)
  def dot(v: Vector3): Float = (x * v.x) + (y * v.y) + (z * v.z)
  def cross(v: Vector3) = Vector3(
      (y * v.z) - (z * v.y),
      (z * v.x) - (x * v.z),
      (x * v.y) - (y * v.x))
  def +(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
  def -(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
}