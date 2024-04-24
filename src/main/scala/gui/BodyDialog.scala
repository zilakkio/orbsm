package gui

import engine.{Body, Vector3D}
import scalafx.Includes.*
import scalafx.scene.control.{ButtonBar, ButtonType, Dialog, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane}
import scalafx.geometry.Insets
import scalafx.stage.{Stage, StageStyle}
import tools.{Settings, Simulation}

class BodyDialog(
                  ownerStage: Stage,
                  body: Body
                 ) extends Dialog[Option[Boolean]]:
  initOwner(ownerStage)
  headerText = body.name

  val createButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OKDone)
  val cancelButtonType = ButtonType.Cancel

  dialogPane().buttonTypes = Seq(createButtonType, cancelButtonType)

  val xPosField = new TextField { text = body.positionAU.x.toString; promptText = "x, AU" }
  val yPosField = new TextField { text = body.positionAU.y.toString; promptText = "y, AU" }
  val zPosField = new TextField { text = body.positionAU.z.toString; promptText = "z, AU" }

  val xVelField = new TextField { text = body.velocity.x.toString; promptText = "Vx, m/s" }
  val yVelField = new TextField { text = body.velocity.y.toString; promptText = "Vy, m/s" }
  val zVelField = new TextField { text = body.velocity.z.toString; promptText = "Vz, m/s" }

  val massField = new TextField { text = body.massEarths.toString; promptText = "m, earths" }
  val radiusField = new TextField { text = body.radiusEarths.toString; promptText = "r, earths" }

  val grid = new GridPane():
    hgap = 10
    vgap = 10
    padding = Insets(20, 20, 10, 10)

    add(new Label("Position, AU:"), 0, 0)
    add(xPosField, 1, 0)
    add(yPosField, 2, 0)
    add(zPosField, 3, 0)

    add(new Label("Velocity, m/s:"), 0, 1)
    add(xVelField, 1, 1)
    add(yVelField, 2, 1)
    add(zVelField, 3, 1)

    add(new Label("Mass, earths:"), 0, 2)
    add(massField, 1, 2)

    add(new Label("Radius, earths:"), 0, 3)
    add(radiusField, 1, 3)

  val labelColumn = new ColumnConstraints()
  labelColumn.minWidth = 120

  val inputColumn = new ColumnConstraints()
  inputColumn.minWidth = 100

  grid.columnConstraints.addAll(labelColumn, inputColumn, inputColumn, inputColumn)
  dialogPane().content = grid

  resultConverter = dialogButton =>
    if dialogButton == createButtonType then
      body.position = Vector3D(xPosField.text.value.toDoubleOption.getOrElse(0.0), yPosField.text.value.toDoubleOption.getOrElse(0.0), zPosField.text.value.toDoubleOption.getOrElse(0.0)) * Settings.metersPerAU
      body.velocity = Vector3D(xVelField.text.value.toDoubleOption.getOrElse(0.0), yVelField.text.value.toDoubleOption.getOrElse(0.0), zVelField.text.value.toDoubleOption.getOrElse(0.0))
      try
        assert(massField.text.value.toDouble > 0)
        body.mass = massField.text.value.toDouble * Settings.earthMass
      catch
        case _ => AlertManager.alert(f"\"${massField.text.value}\" is not a valid mass")
      try
        assert(radiusField.text.value.toDouble > 0)
        body.radius = radiusField.text.value.toDouble * Settings.earthRadius
      catch
        case _ =>
          AlertManager.alert(f"\"${radiusField.text.value}\" is not a valid radius")

      Some(true)
    else None

  dialogPane().getScene.getStylesheets.add(getClass.getResource(Settings.theme).toExternalForm)
  dialogPane().getScene.getWindow.setOnShowing { event =>
    val window = event.getSource.asInstanceOf[javafx.stage.Window]
    window.asInstanceOf[javafx.stage.Stage].initStyle(StageStyle.Undecorated)
  }

  def process(sim: Simulation) =
    val stop = sim.stopped
    sim.pause()
    val result = this.showAndWait()
    if !stop then sim.play()
    result match
      case Some(Some(true)) => true
      case _ => false

