package tools

import engine.{Body, SimulationSpace, Vector3D}
import scalafx.scene.canvas.Canvas
import tools.Centering.AtBody
import tools.CollisionMode.Merge
import tools.Integrator.SemiImplicitEuler

import scala.collection.mutable.Buffer

/** an intermediary between the GUI and the simulation space, includes the space and some additional parameters
 *
 */
class Simulation:
  var name: String = "New Simulation"
  var fps: Double = 120
  var tpf: Double = 10
  var speed: Double = 1.0
  var targetSpeed: Double = 1.0
  var stopped: Boolean = false
  var reversed: Boolean = false
  var space: SimulationSpace = SimulationSpace()

  val startupTime = System.currentTimeMillis()

  var safeTimeStep = 60.0
  var collisionMode: CollisionMode = Merge
  var integrator: Integrator = SemiImplicitEuler

  var workingFile: String = ""
  var fpsRecords: Buffer[(Long, Double)] = Buffer()

  var selectedBody: Option[Body] = None
  var centering: Centering = Centering.MassCenter
  var zoom: Double = 1.0

  var targetZoom: Double = zoom
  val minPixelsPerAU = 100.0
  val minMetersPerPixel = 1 / minPixelsPerAU * 1.496e11

  /** Convert relative zoom to absolute scale
     */
  def metersPerPixel = minMetersPerPixel / zoom

  /** Scale in pixels per astronomical unit
     */
  def pixelsPerAU = minPixelsPerAU * zoom

  /** Target camera position in pixels (animated to)
     */
  def targetPixelOffset = centeringPosition / metersPerPixel

  var pixelOffset: Vector3D = targetPixelOffset
  var lastFrame = 0L

  var cursorPosition: Vector3D = Vector3D(0.0, 0.0)
  var selectableBody: Option[Body] = None

  var centeringWhenEnteredDrag: Option[Vector3D] = None

  var canvas = new Canvas()

  /** Calculate the average FPS over the last second
     */
  def getAverageFPS =
    var sum = 0.0
    fpsRecords.foreach(record =>
      sum += record._2
    )
    sum / fpsRecords.length

  /** Calculate the average FPS over the last 5 records
     */
  def getFPS =
    fpsRecords.slice(fpsRecords.length - 5, fpsRecords.length).map(_._2).sum / 5.0

  /** Change TPF (ticks per frame, only used by the optimizer)
     */
  def setTPF(newTPF: Double) =
    tpf = newTPF
    if tpf < 1 then tpf = 1

  /** Change the simulation speed
     */
  def setSpeed(newSpeed: Double) =
    targetSpeed = newSpeed

  /** Add an FPS record
     */
  def recordFPS(record: Double) =
    val now = System.currentTimeMillis()
    fpsRecords += now -> record
    fpsRecords = fpsRecords.filter(_._1 >= now - 1000)

  /** Get the camera position (in meters)
     */
  def centeringPosition: Vector3D =
    centering match
      case Centering.MassCenter => space.massCenter
      case Centering.Custom(pos) => pos
      case Centering.AtBody(body) => body.position

  /** Calculate the average FPS over the last 5 records
     */
  def select(body: Body) =
    selectedBody = Some(body)
    centering = Centering.AtBody(body)

  /** Cancel the body selection
     */
  def deselect() =
    selectedBody = None

  /** Delete the selected body
     */
  def deleteSelection() =
    if selectedBody.isDefined then
      space.removeBody(selectedBody.get)
      centering = Centering.Custom(selectedBody.get.position)
      deselect()

  /** Change the zoom
     */
  def setZoom(newZoom: Double) =
    targetZoom = newZoom
    if targetZoom <= 1.2e-15 then targetZoom = 1.2e-15
    if targetZoom >= 2.0e8 then targetZoom = 2.0e8

  /** Reset the zoom to 1.0x (100 px per AU)
     */
  def resetZoom() = setZoom(1.0)

  /** Convert a position in pixels to a position in meters
     */
  def pixelToPosition(pixelX: Double, pixelY: Double) =
    (Vector3D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + pixelOffset) * metersPerPixel

  /** Convert a position in pixels to a position in meters (vector version)
     */
  def vPixelToPosition(pixel: Vector3D): Vector3D =
    pixelToPosition(pixel.x, pixel.y)

  /** Convert a position in meters to a position in pixels
     */
  def positionToPixel(position: Vector3D) =
    (Vector3D(position.x / metersPerPixel + canvas.width.value / 2, position.y / metersPerPixel + canvas.height.value / 2)) - pixelOffset

  /** Get the current camera movement velocity
     */
  def cameraVelocity: Vector3D = centering match
    case AtBody(body) => body.velocity
    case _ => Vector3D(0.0, 0.0)

  /** Get the current camera movement acceleration
     */
  def cameraAcceleration: Vector3D = centering match
    case AtBody(body) => body.acceleration
    case _ => Vector3D(0.0, 0.0)

  /** Get the actual simulation time step
     */
  def timestep = 86400.0 * speed / (tpf * getAverageFPS)

  /** Advance the simulation
     */
  def tick(deltaTime: Double) =
    if targetSpeed < 1 / 86400.0 then targetSpeed = 1 / 86400.0
    if speed < 1 / 86400.0 then speed = 1 / 86400.0

    if System.currentTimeMillis() - startupTime > 1000 then
      tpf *= timestep / safeTimeStep

    if getAverageFPS > fps && tpf >= 1 then // high fps
      tpf *= 1.01

    if targetSpeed < speed || fpsRecords.last._2 > fps then
      val deltaSpeed = 0.05 * (targetSpeed - speed)
      speed += deltaSpeed
    if getAverageFPS < fps then
      targetSpeed = speed * math.pow(0.97, fps / getAverageFPS)

    if !stopped then
      val ticksPerFrame = tpf.toInt
      val targetTimeStep = deltaTime * speed * 86400 / ticksPerFrame
      val timeStep = math.min(targetTimeStep, safeTimeStep)
      for i <- 1 to ticksPerFrame do
        space.tick((if reversed then -1 else 1) * timeStep, integrator, collisionMode)
        space.bodies.foreach(_.updateTrail(timeStep))

  def pause() =
    stopped = true

  def play() =
    stopped = false
