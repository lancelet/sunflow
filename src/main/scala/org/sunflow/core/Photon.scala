package org.sunflow.core

import org.sunflow.image.Color
import org.sunflow.math.Point3
import org.sunflow.math.Vector3

// TODO: Eventually, just stack a Ray and Color inside a Photon.
final case class Photon(position: Point3, direction: Vector3, power: Color)

object PhotonJ {
  def create(position: Point3, direction: Vector3, power: Color): Photon =
    new Photon(position, direction, power)
}
