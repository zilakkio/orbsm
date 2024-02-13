import scalafx.scene.paint.Color

import scala.collection.mutable

/** Represents a celestial body in a 2D space.
 *
 * @param position A position vector of the body.
 */
class Body(var position: Vector2D, var name: String = "Body"):
  var velocity = Vector2D(0.0, 0.0)
  var acceleration = Vector2D(0.0, 0.0)
  var mass: Double = 1.0
  var radius: Double = 1.0
  var color: Color = Color.White

  var orbitPrecision = 10
  val positionHistory = mutable.Buffer[Vector2D](position)
  var orbitPrecisionCounter = 0

  /** Update the position based on current velocity
   * @param deltaTime Simulation time step.
   * @return
   */
  def updatePosition(deltaTime: Double) =
    position += velocity * deltaTime
    orbitPrecisionCounter += 1

    if orbitPrecisionCounter % orbitPrecision == 0 then
      positionHistory += position
      if positionHistory.length >= 255 then positionHistory.remove(0)

   /** Update the velocity based on current acceleration
   * @param deltaTime Simulation time step.
   * @return
   */
  def updateVelocity(deltaTime: Double) =
    velocity += acceleration * deltaTime

  /** Update the acceleration based on the total force
   * @param totalForce Total force applied to the body.
   * @return
   */
  def updateAcceleration(totalForce: Vector2D) =
    acceleration = (1 / mass) * totalForce

  /** Update the position based on current velocity
   * @param other Other body to check a collision with.
   * @return true if two bodies are close enough. false otherwise
   */
  def checkCollision(other: Body): Boolean =
    (this.position - other.position).norm <= this.radius + other.radius

  def positionAU = position / 149597870700.0

  def massEarths = mass / 5.9722e24

  def radiusEarths = radius / 6371000

  override def toString: String = s"\n\n$name\nr = ${positionAU.roundPlaces(3)} AU\nv = ${velocity.roundPlaces(1)} m/s\na = ${acceleration.roundPlaces(5)} m/s/s\nmass = ${massEarths.roundPlaces(5)} earths\nradius = ${radiusEarths.roundPlaces(3)} earth"
