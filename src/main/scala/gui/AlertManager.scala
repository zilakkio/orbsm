package gui

import scalafx.scene.canvas.Canvas
import scalafx.scene.control.{Alert, ChoiceDialog, Dialog}
import scalafx.scene.control.Alert.{AlertType, sfxAlert2jfx}
import scalafx.scene.paint.Color
import scalafx.stage.{StageStyle, Window}
import tools.Settings

import scala.collection.mutable.Buffer

/*
object AlertManager:
  var stage: Option[Window] = None
  var message: Option[String] = None

  def alert(text: String) =
    message = Some(text)

  def get(): Option[Alert] =
    message match
      case Some(m) =>
        val alert = new Alert(AlertType.Error) {
          initOwner(stage.get)
          title = "Error"
          headerText = "Something went wrong!"
          contentText = m
          dialogPane().getScene.getWindow.setOnShowing { event =>
            val window = event.getSource.asInstanceOf[javafx.stage.Window]
            window.asInstanceOf[javafx.stage.Stage].initStyle(StageStyle.Undecorated)
          }
        }
        alert.dialogPane().getStylesheets.add(getClass.getResource(Settings.theme).toExternalForm)
        message = None
        Some(alert)
      case None => None
*/

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
