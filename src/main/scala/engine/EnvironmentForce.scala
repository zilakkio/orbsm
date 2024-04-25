package engine

/** A force applied to all objects individually, e.g. friction or air resistance.
 *
 */
class EnvironmentForce(name: String) extends Force(name):
  
  def calculate(target: Body): Vector3D = Vector3D(0.0, 0.0)

  override def toString: String = s"(env force) $name"