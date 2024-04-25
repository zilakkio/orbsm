package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{Button, ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import engine.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.{Settings, Simulation}

/** Side panel section for the selected body
 */
class BodyPanel(val body: Body) extends GridPane:

  padding = Insets(20, 20, 10, 10)

  val title = new Label:
    text = "Selected Body"
    font = Settings.fontTitle

  val nameField = new TextField:
    text = body.name
    text.onChange((_, _, newValue) =>
      body.name = newValue
    )

  val massField = new TextField:
    text = (body.mass / Settings.earthMass).toString

    def updateMass() =
      try
        assert(text().toDouble > 0)
        body.mass = text().toDouble * Settings.earthMass
      catch case _ => AlertManager.alert(f"\"${text.value}\" is not a valid mass")
      text = body.massEarths.toString

    focused.onChange((_, _, _) =>
      updateMass()
    )
    onKeyReleased = (event) =>
      if event.getCode.toString == "ENTER" then updateMass()

  val radiusField = new TextField:
    text = (body.radius / Settings.earthRadius).toString

    def updateRadius() =
      try
        assert(text().toDouble >= 0)
        body.radius = text().toDouble * Settings.earthRadius
      catch case _ =>
        AlertManager.alert(f"\"${text.value}\" is not a valid radius")
        text = body.radiusEarths.toString

    focused.onChange((_, _, _) =>
      updateRadius()
    )
    onKeyReleased = (event) =>
      if event.getCode.toString == "ENTER" then updateRadius()


  val velocityField = new TextField:
    text = body.velocity.norm.toString
    disable = true

    def updateSpeed() =
      try
        assert(text().toDouble >= 0)
        if body.velocity.norm != 0 then
          body.velocity *= text().toDouble / body.velocity.norm
        else
          AlertManager.alert("Direction is undefined")
      catch case _ => AlertManager.alert(f"\"${text.value}\" is not a valid speed")
      text = body.velocity.norm.toString

    focused.onChange((_, _, _) =>
      updateSpeed()
    )
    onKeyReleased = (event) =>
      if event.getCode.toString == "ENTER" then updateSpeed()

  val velocityButtonInv = new Button:
    padding = Insets(5, 5, 5, 5)
    text = "<<"
    font = Settings.fontMono
    onAction = (event) =>
      body.velocity *= -1

  val velocityButton0 = new Button:
    padding = Insets(5, 5, 5, 5)
    text = "×0"
    font = Settings.fontMono
    onAction = (event) =>
      body.velocity = Vector3D(0.0, 0.0)

  val velocityButton05 = new Button:
    padding = Insets(5, 5, 5, 5)
    text = "÷2"
    font = Settings.fontMono
    onAction = (event) =>
      body.velocity *= 0.5

  val velocityButton2 = new Button:
    padding = Insets(5, 5, 5, 5)
    text = "×2"
    font = Settings.fontMono
    onAction = (event) =>
      body.velocity *= 2

  val colorPicker = new ColorPicker(body.color):
    value.onChange((_, _, newValue) =>
      body.color = jfxColor2sfx(newValue)
    )

  /** Update the panel based on the simulation data
     */
  def update(sim: Simulation) =
    if sim.stopped then
      velocityField.disable = false
    else
      velocityField.disable = true
      velocityField.text = body.velocity.norm.toString

  /** Reload the fields
     */
  def fullUpdate() =
    massField.text = body.massEarths.toString
    radiusField.text = body.radiusEarths.toString
    velocityField.text = body.velocity.norm.toString

  this.hgap = 10
  this.vgap = 10

  val labelColumn = new ColumnConstraints()
  labelColumn.minWidth = 120

  val inputColumn = new ColumnConstraints()
  inputColumn.minWidth = 30

  columnConstraints.addAll(labelColumn, inputColumn, inputColumn, inputColumn, inputColumn)

  add(title, 0, 0, 5, 1)

  add(nameField, 0, 1, 5, 1)

  add(new Label("Mass, earths:") {
    graphic = Icons.get("weight")
  }, 0, 2)
  add(massField, 1, 2, 4, 1)

  add(new Label("Radius, earths:") {
    graphic = Icons.get("radius")
  }, 0, 3)
  add(radiusField, 1, 3, 4, 1)

  add(new Label("Speed, m/s:") {
    graphic = Icons.get("v")
  }, 0, 4)
  add(velocityField, 1, 4, 4, 1)

  add(velocityButtonInv, 1, 5)
  add(velocityButton0, 2, 5)
  add(velocityButton05, 3, 5)
  add(velocityButton2, 4, 5)

  add(new Label("Color:") {
    graphic = Icons.get("color")
  }, 0, 6)
  add(colorPicker, 1, 6, 4, 1)
