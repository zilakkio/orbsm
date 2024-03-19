package engine

import scalafx.scene.paint.Color
import tools.Settings

import scala.collection.mutable

/** Represents a celestial body in a 2D space.
 *
 * @param position A position vector of the body.
 */
class Body(var position: Vector3D, var name: String = "Body"):   // m
  var velocity = Vector3D(0.0, 0.0)         // m/s
  var acceleration = Vector3D(0.0, 0.0)     // m/s/s
  var mass: Double = 1.0                    // kg
  var radius: Double = 1.0                  // m
  var color: Color = Color.White
  val positionHistory = mutable.Buffer[Vector3D](position)
  var orbitPrecision: Int = 1
  private var orbitPrecisionCounter = 0

  /** Update the position history based on new position
   * @param deltaTime Simulation time step.
   * @return
   */
  def updateTrail(deltaTime: Double) =
    orbitPrecisionCounter += 1
    if orbitPrecisionCounter % orbitPrecision == 0 then
      positionHistory += position
      if positionHistory.length >= 255 then positionHistory.remove(0)
  
  /** Update the acceleration based on the total force
   * @param totalForce Total force applied to the body.
   * @return
   */
  def updateAcceleration(totalForce: Vector3D) =
    acceleration = (1 / mass) * totalForce

  /** Update the position based on current velocity
   * @param other Other body to check a collision with.
   * @return true if two bodies are close enough. false otherwise
   */
  def checkCollision(other: Body): Boolean =
    (this.position - other.position).norm <= this.radius + other.radius

  def setOrbitPrecision(value: String) =
    orbitPrecision = value.toInt

  def setOrbitPrecision(value: Double) =
    orbitPrecision = value.toInt
    orbitPrecision = if orbitPrecision != 0 then orbitPrecision else 1

  def positionAU = position / Settings.metersPerAU

  def massEarths = mass / Settings.earthMass

  def radiusEarths = radius / Settings.earthRadius

  override def toString: String = f"\n\n$name\n" +
    f"r = [ ${positionAU.x}%.3f ${positionAU.y}%.3f ${positionAU.z}%.3f ] AU\n" +
    f"v = [ ${velocity.x}%.1f ${velocity.y}%.1f ${velocity.z}%.1f ] m/s\n" +
    f"a = [ ${acceleration.x * 1000}%.5f ${acceleration.y * 1000}%.5f ${acceleration.z * 1000}%.5f ] mm/s/s\n" +
    f"mass = ${massEarths}%.5f earths\n" +
    f"radius = ${radiusEarths}%.3f earth"
