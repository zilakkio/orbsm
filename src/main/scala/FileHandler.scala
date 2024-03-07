import play.api.libs.json._

/** A utility object for handling file I/O
 *
 */
object FileHandler:

  /** Create/update a simulation file
   * @param fileName The target file relative path.
   * @param simulation The simulation to be saved.
   * @return
   */
  def save(fileName: String, simulation: Simulation) = ???

  /** Restore a simulation from file
   * @param fileName The source file relative path.
   * @return The restored simulation.
   */
  def load(fileName: String): Simulation = ???