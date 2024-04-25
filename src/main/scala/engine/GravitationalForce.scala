package engine

import scala.math.pow

/** The default gravity implementation
 *
 */
object GravitationalForce extends InteractionForce("GravitationalForce"):
  val G: Double = 6.6743e-11  // gravitational constant
  
  /** Calculate the 1st cosmic velocity for the target exerted from the source, in m/s, as a vector
     */
  override def firstCosmicVelocity(target: Body, source: Body): Vector3D =
    val norm = math.sqrt(G * source.mass / (source.radius + (target.position - source.position).norm))
    val orth = (target.position - source.position).orthogonal
    orth.unit * norm
  
  /** Calculate the gravitational force between two bodies, in newtons
     */
  override def calculate(target: Body, source: Body): Vector3D =
    val distanceVector = (source.position - target.position)
    (distanceVector * G * target.mass * source.mass / pow(distanceVector.norm, 3))