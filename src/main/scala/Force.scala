/** A force that can be applied to a celestial body.
 *
 */
trait Force(val name: String):  
  override def toString: String = s"(force) $name"