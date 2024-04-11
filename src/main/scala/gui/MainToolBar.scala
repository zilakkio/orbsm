package gui
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, Label, Separator, ToggleButton, ToggleGroup, ToolBar}
import tools.MultiSelectMode.{Closest2D, Closest3D, Heaviest, Largest}
import tools.{Settings, Tool}

class MainToolBar extends ToolBar:

  padding = Insets(10, 10, 10, 10)

  val toolSelector = ToggleGroup()

  val nothingSelector = new ToggleButton():
    graphic = Icons.get("drag-move")
    toggleGroup = toolSelector
    selected = true
    onAction = (event) => Settings.tool = Tool.Nothing

  val freeBodySelector = new ToggleButton():
    graphic = Icons.get("circle")
    toggleGroup = toolSelector
    onAction = (event) => Settings.tool = Tool.FreeBody

  val autoOrbitSelector = new ToggleButton():
    graphic = Icons.get("focus")
    toggleGroup = toolSelector
    onAction = (event) => Settings.tool = Tool.AutoOrbit

  val multiSelector = new ToggleGroup()

  val closest2DSelector = new ToggleButton():
    graphic = Icons.get("distance")
    toggleGroup = multiSelector
    selected = true
    onAction = (event) => Settings.multiSelectMode = Closest2D

/*  val closest3DSelector = new ToggleButton("p3"):
    toggleGroup = multiSelector
    onAction = (event) => Settings.multiSelectMode = Closest3D*/

  val heaviestSelector = new ToggleButton():
    graphic = Icons.get("weight")
    toggleGroup = multiSelector
    onAction = (event) => Settings.multiSelectMode = Heaviest

  val largestSelector = new ToggleButton():
    graphic = Icons.get("radius")
    toggleGroup = multiSelector
    onAction = (event) => Settings.multiSelectMode = Largest

  def sep = new Separator():
    prefWidth = 70
    visible = false

  def minisep = new Separator():
    prefWidth = 10
    visible = false

  val vectorVelocityToggle = new ToggleButton():
    graphic = Icons.get("v")
    selected = Settings.showVelocityVectors
    onAction = (event =>
      Settings.showVelocityVectors = selected.value
    )

  val vectorAccelerationToggle = new ToggleButton():
    graphic = Icons.get("a")
    selected = Settings.showAccelerationVectors
    onAction = (event =>
      Settings.showAccelerationVectors = selected.value
    )

  val trailToggle = new ToggleButton():
    graphic = Icons.get("orbit")
    selected = Settings.showTrails
    onAction = (event =>
      Settings.showTrails = selected.value
    )

  val gridToggle = new ToggleButton():
    graphic = Icons.get("grid")
    selected = Settings.showGrid
    onAction = (event =>
      Settings.showGrid = selected.value
    )

  val infoToggle = new ToggleButton():
    graphic = Icons.get("info")
    selected = Settings.showInfo
    onAction = (event =>
      Settings.showInfo = selected.value
    )

  items = ObservableBuffer(minisep, Label("Place"), minisep, freeBodySelector, autoOrbitSelector, nothingSelector, sep, Label("Select"), minisep,
      closest2DSelector, heaviestSelector, largestSelector, sep, Label("Display"), minisep,
      vectorVelocityToggle, vectorAccelerationToggle, trailToggle, gridToggle, infoToggle, sep, Label("View"), minisep,
  )
  
  def update() =
    Settings.tool match
        case Tool.Nothing => nothingSelector.selected = true
        case Tool.FreeBody => freeBodySelector.selected = true
        case Tool.AutoOrbit => autoOrbitSelector.selected = true