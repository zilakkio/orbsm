package gui

import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Window

object AlertManager:
  var stage: Option[Window] = None
  
  def alert(message: String) =
    new Alert(AlertType.Error) {
      initOwner(stage.get)
      title = "Error"
      headerText = "Something went wrong."
      contentText = message
    }.showAndWait()