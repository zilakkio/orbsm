package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import tools.Simulation
import engine.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font

class SimPanel(val sim: Simulation) extends GridPane:

    padding = Insets(20, 20, 10, 10)

    val title = new Label:
      text = "Simulation"
      font = Font(32)

    val nameField = new TextField:
      text = sim.name
      focused.onChange((_, _, _) =>
        sim.name = text()
      )

    val fpsField = new TextField:
      text = sim.fps.toString
      focused.onChange((_, _, _) =>
        sim.fps = text().toInt
      )

    this.hgap = 10
    this.vgap = 10

    val labelColumn = new ColumnConstraints()
    labelColumn.minWidth = 120

    val inputColumn = new ColumnConstraints()
    inputColumn.minWidth = 100

    columnConstraints.addAll(labelColumn, inputColumn)

    add(title, 0, 0, 2, 1)

    add(nameField, 0, 1, 2, 1)

    add(Label("Target FPS:"), 0, 2)
    add(fpsField, 1, 2)
