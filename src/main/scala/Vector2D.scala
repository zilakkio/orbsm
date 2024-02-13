import scala.annotation.targetName
import scala.math.{sqrt, subtractExact}

case class Vector2D(val x: Double, val y: Double):

  @targetName("add")
  def +(other: Vector2D): Vector2D = Vector2D(other.x + x, other.y + y)

  @targetName("sub")
  def -(other: Vector2D): Vector2D = Vector2D(- other.x + x, - other.y + y)

  @targetName("mul")
  def *(scalar: Double): Vector2D = Vector2D(scalar * x, scalar * y)
  
  @targetName("div")
  def /(scalar: Double): Vector2D = 1 / scalar * this

  def dot(other: Vector2D): Double = x * other.x + y * other.y

  def norm: Double = sqrt(x * x + y * y)

  def orthogonal: Vector2D = Vector2D(-y, x)
  
  def unit: Vector2D = this / this.norm

  override def toString: String = f"[$x $y]"

extension (scalar: Double)
  @targetName("mulr")
  def *(vector2D: Vector2D) = vector2D * scalar
