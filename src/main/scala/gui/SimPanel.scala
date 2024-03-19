package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{CheckBox, ChoiceBox, ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import tools.{CollisionMode, Integrator, Settings, Simulation}
import engine.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.CollisionMode.{Bounce, Disabled, Merge}
import tools.Integrator.{ExplicitEuler, RK2, SemiImplicitEuler, Verlet}

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

    val collisionField = new ChoiceBox[CollisionMode]:
      prefWidth = 150
      value = sim.collisionMode
      items = ObservableBuffer(Merge, Bounce, Disabled)

      focused.onChange((_, _, _) =>
        sim.collisionMode = value.value
      )

    val integratorField = new ChoiceBox[Integrator]:
      prefWidth = 150
      value = sim.integrator
      items = ObservableBuffer(ExplicitEuler, SemiImplicitEuler, Verlet, RK2)

      focused.onChange((_, _, _) =>
        sim.integrator = value.value
      )

    val vectorVelocityCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showVelocityVectors
      focused.onChange((_, _, _) =>
        sim.showVelocityVectors = selected.value
      )

    val vectorAccelerationCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showAccelerationVectors
      focused.onChange((_, _, _) =>
        sim.showAccelerationVectors = selected.value
      )

    val trailCheckbox = new CheckBox():
      allowIndeterminate = false
      selected = sim.showTrails
      focused.onChange((_, _, _) =>
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

    add(Label("Collisions:"), 0, 4)
    add(collisionField, 1, 4)

    add(Label("Integrator:"), 0, 5)
    add(integratorField, 1, 5)

    add(Label("v-vectors:"), 0, 6)
    add(vectorVelocityCheckbox, 1, 6)

    add(Label("a-vectors:"), 0, 7)
    add(vectorAccelerationCheckbox, 1, 7)
