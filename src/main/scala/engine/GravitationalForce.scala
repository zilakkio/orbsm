package engine

import scala.math.pow

/** The default gravity implementation
 *
 */
object GravitationalForce extends InteractionForce("GravitationalForce"):
  val G: Double = 6.6743e-11  // gravitational constant

  override def firstCosmicVelocity(target: Body, source: Body): Vector3D =
    val norm = math.sqrt(G * source.mass / (source.radius + (target.position - source.position).norm))
    val orth = (target.position - source.position).orthogonal
    orth.unit * norm

  override def calculate(target: Body, source: Body): Vector3D =
    val distanceVector = (source.position - target.position)
    (distanceVector * G * target.mass * source.mass / pow(distanceVector.norm, 3))