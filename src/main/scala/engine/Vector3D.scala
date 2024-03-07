package engine

import scala.annotation.targetName
import scala.math.sqrt

case class Vector3D(val x: Double, val y: Double, val z: Double = 0):

  @targetName("add")
  def +(other: Vector3D): Vector3D = Vector3D(other.x + x, other.y + y, other.z + z)

  @targetName("sub")
  def -(other: Vector3D): Vector3D = Vector3D(- other.x + x, - other.y + y, - other.z + z)

  @targetName("mul")
  def *(scalar: Double): Vector3D = Vector3D(scalar * x, scalar * y, scalar * z)
  
  @targetName("div")
  def /(scalar: Double): Vector3D = 1 / scalar * this

  def dot(other: Vector3D): Double = x * other.x + y * other.y + z * other.z

  def norm: Double = sqrt((x * x) + (y * y) + (z * z))

  def orthogonal: Vector3D = Vector3D(-y, x, z)

  def unit: Vector3D = this / this.norm

  def roundPlaces(places: Int) = Vector3D(x.roundPlaces(places), y.roundPlaces(places), z.roundPlaces(places))

  def round = Vector3D(x.round, y.round, z.round)

  override def toString: String = f"[$x $y $z]"

extension (scalar: Double)
  @targetName("mulr")
  def *(vector2D: Vector3D) = vector2D * scalar

  def roundPlaces(places: Int) =
    val k = math.pow(10, places)
    (scalar * k).round / k
