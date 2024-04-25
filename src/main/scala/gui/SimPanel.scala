package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{CheckBox, ChoiceBox, ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import tools.{CollisionMode, Integrator, Settings, Simulation}
import engine.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.CollisionMode.{Disabled, Merge}
import tools.Integrator.{ExplicitEuler, RK2, RK4, SemiImplicitEuler, Verlet}

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
      text = sim.fps.toInt.toString
      def updateFPS() =
        try
          assert(text().toDouble.toInt >= 0)
          sim.fps = text().toDouble.toInt
        catch
          case _ =>
            AlertManager.alert("Target FPS should be a non-negative number")
            text = sim.fps.toInt.toString
      focused.onChange((_, _, _) =>
        updateFPS()
      )
      onKeyReleased = (event) =>
        if event.getCode.toString == "ENTER" then updateFPS()

    val timestepField = new TextField:
      text = sim.safeTimeStep.toString
      def updateTimestep() =
        try
          assert(text().toDouble > 0)
          sim.safeTimeStep = text().toDouble
        catch
          case _ =>
            AlertManager.alert("Timestep should be a positive number")
            text = sim.safeTimeStep.toString
      focused.onChange((_, _, _) =>
        updateTimestep()
      )
      onKeyReleased = (event) =>
        if event.getCode.toString == "ENTER" then updateTimestep()

    val dragField = new TextField:
      text = sim.space.drag.k.toString
      def updateDrag() =
        try
          assert(text().toDouble >= 0)
          sim.space.drag.k = text().toDouble
        catch
          case _ =>
            AlertManager.alert("Drag coefficient should be a non-negative number")
            text = sim.space.drag.k.toString
      focused.onChange((_, _, _) =>
        updateDrag()
      )
      onKeyReleased = (event) =>
        if event.getCode.toString == "ENTER" then updateDrag()

    val collisionField = new ChoiceBox[CollisionMode]:
      prefWidth = 150
      value = sim.collisionMode
      items = ObservableBuffer(Merge, Disabled)

      onAction = (event =>
        sim.collisionMode = value.value
      )

    val integratorField = new ChoiceBox[Integrator]:
      prefWidth = 150
      value = sim.integrator
      items = ObservableBuffer(ExplicitEuler, SemiImplicitEuler, Verlet, RK2, RK4)

      onAction = (event =>
        sim.integrator = value.value
      )

    this.hgap = 10
    this.vgap = 10

    val labelColumn = new ColumnConstraints()
    labelColumn.minWidth = 150

    val inputColumn = new ColumnConstraints()
    inputColumn.minWidth = 135
    inputColumn.maxWidth = 135

    columnConstraints.addAll(labelColumn, inputColumn)

    add(title, 0, 0, 2, 1)

    add(nameField, 0, 1, 2, 1)

    add(new Label("Target FPS:"), 0, 2)
    add(fpsField, 1, 2)

    add(new Label("Safe timestep, s:") {graphic = Icons.get("time")}, 0, 3)
    add(timestepField, 1, 3)

    add(new Label("Drag:"), 0, 4)
    add(dragField, 1, 4)

    add(new Label("Collisions:"), 0, 5)
    add(collisionField, 1, 5)

    add(new Label("Integrator:") {graphic = Icons.get("formula")}, 0, 6)
    add(integratorField, 1, 6)
