package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{ColorPicker, Label, TextField}
import scalafx.scene.layout.{HBox, VBox}
import engine.*
import scalafx.geometry.Pos
import scalafx.scene.text.Font

class BodyPanel(val body: Body) extends VBox:
    val title = new Label:
      text = "Selected Body"
      font = Font(32)

    val nameField = new TextField:
      text = body.name
      text.onChange((_, _, newValue) =>
        body.name = newValue
      )
      prefWidth = 100

    val massField = new TextField:
      text = (body.mass / 5.9722e24).toString
      focused.onChange((_, _, _) =>
        body.mass = text().toDouble * 5.9722e24
      )

    val massHBox = new HBox(new Label("Mass, earths:"), massField):
      spacing = 10
      alignment = Pos.CenterLeft

    val radiusField = new TextField:
      text = (body.radius / 6371000.0).toString
      focused.onChange((_, _, _) =>
        body.radius = text().toDouble * 6371000.0
      )
      prefWidth = 100

    val radiusHBox = new HBox(new Label("Radius, earths:"), radiusField):
      spacing = 10
      alignment = Pos.CenterLeft

    val colorPicker = new ColorPicker(body.color):
      value.onChange((_, _, newValue) =>
        body.color = jfxColor2sfx(newValue)
      )

    val colorHBox = new HBox(new Label("Color:"), colorPicker):
      spacing = 10
      alignment = Pos.CenterLeft

    val orbitPrecisionField = new TextField:
      text = body.orbitPrecision.toString
      focused.onChange((_, _, _) =>
        body.orbitPrecision = text().toInt
      )

    val orbitHBox = new HBox(new Label("Orbit Precision:"), orbitPrecisionField):
      spacing = 10
      alignment = Pos.CenterLeft

    this.spacing = 10
    this.children = Array(
      title,
      nameField,
      massHBox,
      radiusHBox,
      colorHBox,
      orbitHBox
    )