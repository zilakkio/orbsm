package gui

import scalafx.scene.canvas.Canvas
import scalafx.scene.control.{Alert, ChoiceDialog, Dialog}
import scalafx.scene.control.Alert.{AlertType, sfxAlert2jfx}
import scalafx.scene.paint.Color
import scalafx.stage.{StageStyle, Window}
import tools.Settings

import scala.collection.mutable.Buffer


object AlertManager:
  var alerts: Buffer[AlertInstance] = Buffer()

  def alert(text: String, color: Color = Color.Red) =
    alerts += AlertInstance(text, color, System.currentTimeMillis())

  def update() =
    val now = System.currentTimeMillis()
    alerts = alerts.filter(_.time > (now - 3000))

  def get(): Buffer[AlertInstance] =
    update()
    alerts
