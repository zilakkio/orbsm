import scala.collection.mutable.*

/** A 2D space where all the bodies are placed and interact.
 *
 */
class SimulationSpace:
  val bodies = Buffer[Body]()
  var interactionForces = Vector[InteractionForce]()
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
  def addAutoOrbit(body: Body, parent: Body) =
    interactionForces.foreach(force =>
      body.velocity += force.firstCosmicVelocity(body, parent)
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
  def calculateTotalForce(body: Body): Vector2D =
    var totalForce = Vector2D(0.0, 0.0)

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
  def tick(deltaTime: Double) =

    // semi-implicit euler
    bodies.foreach(body =>
      val totalForce = calculateTotalForce(body)  // N
      body.updateAcceleration(totalForce)  // m/s^2
      body.updateVelocity(deltaTime)  // m/s
    )
    bodies.foreach(body =>
      body.updatePosition(deltaTime)  // m
    )

  /** Check for collisions between the bodies and handle them.
   * @return
   */
  def updateCollisions() = ???
  
  def massCenter: Vector2D =
    var center = Vector2D(0.0, 0.0)
    var totalMass = 0.0
    bodies.foreach(body =>
      totalMass += body.mass
      center += body.mass * body.position
    )
    center / totalMass
