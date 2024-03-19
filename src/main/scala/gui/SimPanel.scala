package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{CheckBox, ChoiceBox, ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import tools.{CollisionMode, Integrator, Settings, Simulation}
import engine.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.CollisionMode.{Disabled, Elastic, Inelastic, Merge}
import tools.Integrator.{ExplicitEuler, RK2, RK4, Random, SemiImplicitEuler, Verlet}

class SimPanel(val sim: Simulation) extends GridPane:

    padding = Insets(20, 20, 10, 10)

    val title = new Label:
      text = "Simulation"
      font = Settings.fontTitle

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

    val timestepField = new TextField:
      text = sim.safeTimeStep.toString
      focused.onChange((_, _, _) =>
        sim.safeTimeStep = text().toDouble
      )

    val speedField = new TextField:
      text = sim.speed.toString
      focused.onChange((_, _, _) =>
        sim.speed = text().toDouble
      )

    val collisionField = new ChoiceBox[CollisionMode]:
      prefWidth = 150
      value = sim.collisionMode
      items = ObservableBuffer(Merge, Elastic, Inelastic, Disabled)

      onAction = (event =>
        sim.collisionMode = value.value
      )

    val integratorField = new ChoiceBox[Integrator]:
      prefWidth = 150
      value = sim.integrator
      items = ObservableBuffer(ExplicitEuler, SemiImplicitEuler, Verlet, RK2, RK4, Random)

      onAction = (event =>
        sim.integrator = value.value
      )

    val vectorVelocityCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showVelocityVectors
      onAction = (event =>
        sim.showVelocityVectors = selected.value
      )

    val vectorAccelerationCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showAccelerationVectors
      onAction = (event =>
        sim.showAccelerationVectors = selected.value
      )

    val trailCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showTrails
      onAction = (event =>
        sim.showTrails = selected.value
      )

    this.hgap = 10
    this.vgap = 10


    val labelColumn = new ColumnConstraints()
    labelColumn.minWidth = 120

    val inputColumn = new ColumnConstraints()
    inputColumn.minWidth = 150
    inputColumn.maxWidth = 150

    columnConstraints.addAll(labelColumn, inputColumn)

    add(title, 0, 0, 2, 1)

    add(nameField, 0, 1, 2, 1)

    add(Label("Target FPS:"), 0, 2)
    add(fpsField, 1, 2)

    add(Label("Safe timestep, s:"), 0, 3)
    add(timestepField, 1, 3)

    add(Label("Speed, days/s:"), 0, 4)
    add(speedField, 1, 4)

    add(Label("Collisions:"), 0, 5)
    add(collisionField, 1, 5)

    add(Label("Integrator:"), 0, 6)
    add(integratorField, 1, 6)

    add(Label("v-vectors:"), 0, 7)
    add(vectorVelocityCheckbox, 1, 7)

    add(Label("a-vectors:"), 0, 8)
    add(vectorAccelerationCheckbox, 1, 8)

    add(Label("Display trails:"), 0, 9)
    add(trailCheckbox, 1, 9)
