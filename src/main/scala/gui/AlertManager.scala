package gui

import scalafx.scene.canvas.Canvas
import scalafx.scene.control.{Alert, ChoiceDialog, Dialog}
import scalafx.scene.control.Alert.{AlertType, sfxAlert2jfx}
import scalafx.scene.paint.Color
import scalafx.stage.{StageStyle, Window}
import tools.Settings

import scala.collection.mutable.Buffer


object AlertManager:
  private var alerts: Buffer[AlertInstance] = Buffer()
  
  /** Create an alert. It will be displayed with the next frame update
     */
  def alert(text: String, color: Color = Color.Red) =
    alerts += AlertInstance(text, color, System.currentTimeMillis())
  
  /** Remove all expired alerts
     */
  def update() =
    val now = System.currentTimeMillis()
    alerts = alerts.filter(_.time > (now - 3000))
  
  /** Return all alerts
     */
  def get(): Buffer[AlertInstance] =
    update()
    alerts
