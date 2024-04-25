package engine


class Drag(var k: Double) extends EnvironmentForce("drag"):

  override def calculate(target: Body): Vector3D =
    target.velocity.unit *
      (- k * target.velocity.norm * target.velocity.norm * target.radius * target.radius)