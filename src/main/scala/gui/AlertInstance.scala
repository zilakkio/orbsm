package gui

import scalafx.scene.paint.Color

case class AlertInstance(var message: String, var color: Color, var time: Long):
  
  /** Calculate the currect notification opacity
     */
  def opacity = 1.0 - (System.currentTimeMillis() - time) / 3000.0
  
  /** Calculate the currect notification shift relative to the final position
     */
  def shift = math.max(400.0 * (1.0 - (System.currentTimeMillis() - time) / 300.0), 0)
