package engine


class Drag(var k: Double) extends EnvironmentForce("drag"):

  /** Calculate the drag for the target, in newtons. F_drag = -k * v^2 * r^2
   * (not accurate numerically and dimensionally)
   */
  override def calculate(target: Body): Vector3D =
    target.velocity.unit *
      (-k * target.velocity.norm * target.velocity.norm * target.radius * target.radius)