package gui

import scalafx.scene.control.{Alert, ChoiceDialog, Dialog}
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.{StageStyle, Window}
import tools.Settings

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