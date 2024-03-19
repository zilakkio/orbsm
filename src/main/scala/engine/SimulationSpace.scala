package engine

import tools.Integrator.SemiImplicitEuler
import tools.{CollisionMode, Integrator}

import scala.collection.mutable
import scala.collection.mutable.*

/** A 2D space where all the bodies are placed and interact.
 *
 */
class SimulationSpace:
  var bodies = Buffer[Body]()
  var interactionForces = Vector[InteractionForce](GravitationalForce)
  var environmentForces = Vector[EnvironmentForce]()

  /** Add a celestial body to the space.
   * @param body A celestial body to be added.
   * @return
   */
  def addBody(body: Body) =
    bodies += body

  /** Add a celestial body to the space and set the velocity to create a round orbit.
   * @param body A celestial body to be added.
   * @param parent A celestial body to add an orbit around.
   * @return
   */
  def addAutoOrbit(body: Body, parent: Body, reverse: Boolean = false) =
    var v = Vector3D(0.0, 0.0)
    interactionForces.foreach(force =>
      body.velocity += force.firstCosmicVelocity(body, parent) * {if reverse then -1 else 1}
    )
    body.velocity += parent.velocity
    addBody(body)

  /** Remove a celestial body from the space.
   *
   * @param body A celestial body to be removed.
   * @return true if the body was in the space and was successfully removed, false otherwise.
   */
  def removeBody(body: Body): Boolean =
    if bodies.contains(body) then
      bodies -= body
      true
    else false

  /** Calculate the total force applied to a body exerted by all other bodies.
   * @param body A celestial body for which the forces are calculated.
   * @return
   */
  def calculateTotalForce(body: Body): Vector3D =
    var totalForce = Vector3D(0.0, 0.0)

    // calculate all interaction forces
    interactionForces.foreach( force =>
      bodies.filter(_ != body).foreach( source =>
        totalForce += force.calculate(body, source)
      )
    )

    // calculate all environment forces
    environmentForces.foreach( force =>
      totalForce += force.calculate(body)
    )

    // return the vector sum of all forces
    totalForce

  /** Advance the simulation.
   * @param deltaTime Simulation time step.
   * @return
   */
  def tick(deltaTime: Double, integrator: Integrator, collisionMode: CollisionMode) =
    integrator match
      case SemiImplicitEuler =>
        // semi-implicit euler
        bodies.foreach(body =>
          val totalForce = calculateTotalForce(body)  // N
          body.updateAcceleration(totalForce)  // m/s^2
          body.updateVelocity(deltaTime)  // m/s
        )
        bodies.foreach(body =>
          body.updatePosition(deltaTime)  // m
        )
        updateCollisions(collisionMode)
      case _ => ???

  /** Check for collisions between the bodies and handle them.
   *
   * @return
   */
  def updateCollisions(mode: CollisionMode) =
    var filtered = bodies
    for
      body1 <- bodies
      body2 <- bodies.filter(_ != body1)
    do
      if (body1.position - body2.position).norm <= (body1.radius + body2.radius) then
        mode match
          case CollisionMode.Merge =>
            if body1.mass >= body2.mass then
              body1.mass += body2.mass
              filtered = bodies.filter(_ != body2)
            else
              body2.mass += body1.mass
              filtered = bodies.filter(_ != body1)
          case CollisionMode.Bounce =>
            body1.velocity = ((body1.mass - body2.mass) * body1.velocity + body2.velocity * 2 * body2.mass) / (body1.mass + body2.mass)
            body2.velocity = ((body2.mass - body1.mass) * body2.velocity + body1.velocity * 2 * body1.mass) / (body1.mass + body2.mass)
          case _ => ()
    bodies = filtered

  def massCenter: Vector3D =
    if bodies.isEmpty then
      return Vector3D(0.0, 0.0)
    var center = Vector3D(0.0, 0.0)
    var totalMass = 0.0
    bodies.foreach(body =>
      totalMass += body.mass
      center += body.mass * body.position
    )
    center / totalMass
