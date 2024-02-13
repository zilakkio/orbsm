import scala.collection.mutable.Buffer

/** an intermediary between the GUI and the simulation space, includes the space and some additional parameters
 *
 */
class Simulation:
  val space: SimulationSpace = SimulationSpace()
  var speed: Double = 1.0
  var fps: Double = 120
  val fpsRecords: Buffer[Double] = Buffer()
  
  def getAverageFPS =
    var sum = 0.0
    fpsRecords.foreach(record =>
      sum += record
    )
    sum / fpsRecords.length
    
  def recordFPS(record: Double) =
    fpsRecords += record
    if fpsRecords.length > 255 then fpsRecords.remove(0)
    
  /** Update the simulation file.
   * @return
   */
  def save() = ???