package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import engine.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.Settings

class BodyPanel(val body: Body) extends GridPane:

  padding = Insets(20, 20, 10, 10)

  val title = new Label:
    text = "Selected Body"
    font = Font(32)

  val nameField = new TextField:
    text = body.name
    text.onChange((_, _, newValue) =>
      body.name = newValue
    )

  val massField = new TextField:
    text = (body.mass / Settings.earthMass).toString
    focused.onChange((_, _, _) =>
      try
        body.mass = text().toDouble * Settings.earthMass
      catch case e => AlertManager.alert(e.toString)
    )

  val radiusField = new TextField:
    text = (body.radius / Settings.earthRadius).toString
    focused.onChange((_, _, _) =>
      body.radius = text().toDouble * Settings.earthRadius
    )

  val colorPicker = new ColorPicker(body.color):
    value.onChange((_, _, newValue) =>
      body.color = jfxColor2sfx(newValue)
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

  add(Label("Mass, earths:"), 0, 2)
  add(massField, 1, 2)

  add(Label("Radius, earths:"), 0, 3)
  add(radiusField, 1, 3)

  add(Label("Color:"), 0, 4)
  add(colorPicker, 1, 4)
