package tools

import engine.{Body, SimulationSpace, Vector3D}
import tools.Centering.AtBody

import scala.collection.mutable.Buffer

/** an intermediary between the GUI and the simulation space, includes the space and some additional parameters
 *
 */
class Simulation:
  var fps: Int = 120
  var tpf: Double = 10
  var speed: Double = 1.0
  var stopped: Boolean = false
  var space: SimulationSpace = SimulationSpace()

  var workingFile: String = ""  
  val fpsRecords: Buffer[Double] = Buffer()

  // GUI-related
  var selectedBody: Option[Body] = None
  var centering: Centering = Centering.MassCenter
  var zoom: Double = 1.0
  var tool: Tool = Tool.Nothing

  var targetZoom: Double = zoom

  def getAverageFPS =
    var sum = 0.0
    fpsRecords.foreach(record =>
      sum += record
    )
    sum / fpsRecords.length

  def setTPF(newTPF: Double) =
    tpf = newTPF
    if tpf < 1 then tpf = 1

  def recordFPS(record: Double) =
    fpsRecords += record
    if fpsRecords.length > 255 then fpsRecords.remove(0)

  def centeringPosition =
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

  def tick(deltaTime: Double) =
    if !stopped then
      for i <- 1 to tpf.toInt do
        space.tick(deltaTime * speed * 86400 * (1.0 / tpf.toInt.toDouble))


  /** Update the simulation file.
   * @return
   */
  def save() = ???