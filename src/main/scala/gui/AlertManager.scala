package gui

import scalafx.scene.control.{Alert, ChoiceDialog, Dialog}
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.Window

object AlertManager:
  var stage: Option[Window] = None
  
  def alert1(message: String) =
    new Alert(AlertType.Error) {
      initOwner(stage.get)
      title = "Error"
      headerText = "Something went wrong."
      contentText = message
    }.showAndWait()

  def alert(message: String) =
    val dialog = new ChoiceDialog(defaultChoice = "b", choices = Seq("a", "b", "c")) {
      initOwner(stage.get)
      title = "Choice Dialog"
      headerText = "Look, a Choice Dialog."
      contentText = "Choose your letter:"
    }
    dialog.showAndWait()