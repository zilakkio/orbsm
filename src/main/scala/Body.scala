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

  var orbitPrecision = 1
  val positionHistory = mutable.Buffer[Vector2D](position)
  private var orbitPrecisionCounter = 0

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

  override def toString: String = f"\n\n$name\n" +
    f"r = [ ${positionAU.x}%.3f ${positionAU.y}%.3f ] AU\n" +
    f"v = [ ${velocity.x}%.1f ${velocity.y}%.1f ] m/s\n" +
    f"a = [ ${acceleration.x * 1000}%.5f ${acceleration.y * 1000}%.5f ] mm/s/s\n" +
    f"mass = ${massEarths}%.5f earths\n" +
    f"radius = ${radiusEarths}%.3f earth"
