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

    val fpsField = new TextField:
      text = sim.fps.toString
      focused.onChange((_, _, _) =>
        sim.fps = text().toInt
      )

    val fpsHBox = new HBox(new Label("FPS:"), fpsField):
      spacing = 10
      alignment = Pos.CenterLeft

    val precisionField = new TextField:
      text = sim.space.bodies.head.orbitPrecision.toString
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
      fpsHBox,
      precisionHBox
    )