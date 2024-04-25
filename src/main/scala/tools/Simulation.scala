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

  // GUI-related
  var selectedBody: Option[Body] = None
  var centering: Centering = Centering.MassCenter
  var zoom: Double = 1.0

  var targetZoom: Double = zoom
  val minPixelsPerAU = 100.0
  val minMetersPerPixel = 1/minPixelsPerAU * 1.496e11

  def metersPerPixel = minMetersPerPixel / zoom
  def pixelsPerAU = minPixelsPerAU * zoom
  def targetPixelOffset = centeringPosition / metersPerPixel

  var pixelOffset: Vector3D = targetPixelOffset
  var lastFrame = 0L

  var cursorPosition: Vector3D = Vector3D(0.0, 0.0)
  var selectableBody: Option[Body] = None

  var centeringWhenEnteredDrag: Option[Vector3D] = None

  var canvas = new Canvas()

  def getAverageFPS =
    var sum = 0.0
    fpsRecords.foreach(record =>
      sum += record._2
    )
    sum / fpsRecords.length

  def getFPS =
    fpsRecords.slice(fpsRecords.length - 5, fpsRecords.length).map(_._2).sum / 5.0

  def setTPF(newTPF: Double) =
    tpf = newTPF
    if tpf < 1 then tpf = 1

  def setSpeed(newSpeed: Double) =
    targetSpeed = newSpeed

  def recordFPS(record: Double) =
    val now = System.currentTimeMillis()
    fpsRecords += now -> record
    fpsRecords = fpsRecords.filter(_._1 >= now - 1000)

  def centeringPosition: Vector3D =
    centering match
      case Centering.MassCenter => space.massCenter
      case Centering.Custom(pos) => pos
      case Centering.AtBody(body) => body.position

  def select(body: Body) =
    selectedBody = Some(body)
    centering = Centering.AtBody(body)

  def deselect() =
    selectedBody = None

  def deleteSelection() =
    if selectedBody.isDefined then
      space.removeBody(selectedBody.get)
      centering = Centering.Custom(selectedBody.get.position)
      deselect()

  def setZoom(newZoom: Double) =
    targetZoom = newZoom
    if targetZoom <= 1.2e-15 then targetZoom = 1.2e-15
    if targetZoom >= 2.0e8 then targetZoom = 2.0e8

  def resetZoom() = setZoom(1.0)
  
  def pixelToPosition(pixelX: Double, pixelY: Double) =
      (Vector3D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + pixelOffset) * metersPerPixel

  def vPixelToPosition(pixel: Vector3D): Vector3D =
    pixelToPosition(pixel.x, pixel.y)

  def positionToPixel(position: Vector3D) =
      (Vector3D(position.x / metersPerPixel + canvas.width.value / 2, position.y / metersPerPixel + canvas.height.value / 2)) - pixelOffset

  def cameraVelocity: Vector3D = centering match
    case AtBody(body) => body.velocity
    case _ => Vector3D(0.0, 0.0)

  def cameraAcceleration: Vector3D = centering match
    case AtBody(body) => body.acceleration
    case _ => Vector3D(0.0, 0.0)

  def timestep = 86400.0 * speed / (tpf * getAverageFPS)

  def tick(deltaTime: Double) =
    if targetSpeed < 1/86400.0 then targetSpeed = 1/86400.0
    if speed < 1/86400.0 then speed = 1/86400.0

    if System.currentTimeMillis() - startupTime > 1000 then
      tpf *= timestep / safeTimeStep

    if getAverageFPS > fps && tpf >= 1 then  // high fps
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
