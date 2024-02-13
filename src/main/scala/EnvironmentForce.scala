/** A force applied to all objects individually, e.g. friction or air resistance.
 *
 */
class EnvironmentForce(name: String) extends Force(name):

  def calculate(target: Body): Vector2D = Vector2D(0.0, 0.0)

  override def toString: String = s"(env force) $name"