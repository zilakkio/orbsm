package engine

import scala.annotation.targetName
import scala.math.sqrt

case class Vector3D(val x: Double, val y: Double, val z: Double = 0):

  @targetName("inv")
  def unary_- = Vector3D(-x, -y, -z)

  @targetName("add")
  def +(other: Vector3D): Vector3D = Vector3D(other.x + x, other.y + y, other.z + z)

  @targetName("sub")
  def -(other: Vector3D): Vector3D = Vector3D(-other.x + x, -other.y + y, -other.z + z)

  @targetName("mul")
  def *(scalar: Double): Vector3D = Vector3D(scalar * x, scalar * y, scalar * z)
  
  @targetName("div")
  def /(scalar: Double): Vector3D = 1 / scalar * this

  def dot(other: Vector3D): Double = x * other.x + y * other.y + z * other.z

  def cross(other: Vector3D): Vector3D = Vector3D(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
  
  /** Vector norm (magnitude)
     */
  def norm: Double = sqrt((x * x) + (y * y) + (z * z))
  
  /** Calculate one of the orthogonal vectors
     */
  def orthogonal: Vector3D = Vector3D(-y, x, z)
  
  /** Normalize
     */
  def unit: Vector3D =
    if this.norm == 0.0 then Vector3D(1.0, 0.0, 0.0) else this / this.norm

  def round = Vector3D(x.round, y.round, z.round)
  
  /** Projection to the XY-plane
     */
  def noz = Vector3D(x, y, 0.0)

  override def toString: String = f"[$x $y $z]"

extension (scalar: Double)
  @targetName("mulr")
  def *(vector2D: Vector3D) = vector2D * scalar