package engine

/** A force applied to a pair of objects, e.g. gravitational force
 *
 */
class InteractionForce(name: String) extends Force(name):
  
  def firstCosmicVelocity(target: Body, source: Body): Vector3D = Vector3D(0.0, 0.0)
  
  def calculate(target: Body, source: Body): Vector3D = Vector3D(0.0, 0.0)
  
  override def toString: String = s"(interaction force) $name"