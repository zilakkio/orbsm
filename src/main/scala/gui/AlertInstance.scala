package gui

import scalafx.scene.paint.Color

case class AlertInstance(var message: String, var color: Color, var time: Long):

  def opacity = 1.0 - (System.currentTimeMillis() - time) / 3000.0
  def shift = math.max(400.0 * (1.0 - (System.currentTimeMillis() - time) / 300.0), 0)
