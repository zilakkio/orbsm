package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{ColorPicker, Label, TextField}
import scalafx.scene.layout.{HBox, VBox}
import tools.Simulation
import engine.*
import scalafx.geometry.Pos
import scalafx.scene.text.Font

class SimPanel(val sim: Simulation) extends VBox:
    val title = new Label:
      text = "Simulation"
      font = Font(32)

    val nameField = new TextField:
      text = sim.name
      focused.onChange((_, _, _) =>
        sim.name = text()
      )

    val nameHBox = new HBox(nameField):
      spacing = 10
      alignment = Pos.CenterLeft

    val fpsField = new TextField:
      text = sim.fps.toString
      focused.onChange((_, _, _) =>
        sim.fps = text().toInt
      )

    val fpsHBox = new HBox(new Label("FPS:"), fpsField):
      spacing = 10
      alignment = Pos.CenterLeft

    val precisionField = new TextField:
      text = if sim.space.bodies.nonEmpty then sim.space.bodies.head.orbitPrecision.toString else "1"
      focused.onChange((_, _, _) =>
        sim.space.bodies.foreach( body =>
          body.orbitPrecision = text().toInt
        )
      )

    val precisionHBox = new HBox(new Label("Orbits precision:"), precisionField):
      spacing = 10
      alignment = Pos.CenterLeft

    this.spacing = 10
    this.children = Array(
      title,
      nameHBox,
      fpsHBox,
      precisionHBox
    )