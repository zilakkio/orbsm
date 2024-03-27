package tools

import engine.{Body, SimulationSpace, Vector3D}
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color.Black
import tools.Centering.AtBody
import tools.CollisionMode.{Elastic, Merge, Disabled}
import tools.Integrator.SemiImplicitEuler

import scala.collection.mutable.Buffer

/** an intermediary between the GUI and the simulation space, includes the space and some additional parameters
 *
 */
class Simulation:
  var name: String = "New Simulation"
  var fps: Int = 120
  var tpf: Double = 10
  var speed: Double = 1.0
  var stopped: Boolean = false
  var space: SimulationSpace = SimulationSpace()

  var safeTimeStep = 60.0
  var collisionMode: CollisionMode = Elastic
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

  def setTPF(newTPF: Double) =
    tpf = newTPF
    if tpf < 1 then tpf = 1

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
    if targetZoom <= 0 then targetZoom = 0.10

  def resetZoom() = setZoom(1.0)

  def cameraVelocity: Vector3D = centering match
    case AtBody(body) => body.velocity
    case _ => Vector3D(0.0, 0.0)

  def cameraAcceleration: Vector3D = centering match
    case AtBody(body) => body.acceleration
    case _ => Vector3D(0.0, 0.0)

  def tick(deltaTime: Double) =
    if !stopped then
      for i <- 1 to tpf.toInt do
        space.tick(deltaTime * speed * 86400 * (1.0 / tpf.toInt.toDouble), integrator, collisionMode)
    space.bodies.foreach(_.updateTrail(deltaTime))

  def pause() =
    stopped = true

  def play() =
    stopped = false
