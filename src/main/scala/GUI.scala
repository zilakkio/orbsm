import tools.Centering.{AtBody, MassCenter}
import tools.Tool.*
import engine.{Body, GravitationalForce, Orbit, Vector3D}
import gui.{AlertManager, BodyDialog, BodyPanel, Icons, MainToolBar, SimPanel}
import scalafx.animation.AnimationTimer
import scalafx.application.{JFXApp3, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.control.*
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.paint.Color
import scalafx.scene.text.{Font, TextAlignment}
import scalafx.Includes.jfxColor2sfx
import scalafx.scene.transform.{Rotate, Translate}
import scalafx.stage.{FileChooser, Stage, StageStyle}
import scalafx.stage.FileChooser.ExtensionFilter
import tools.{Centering, Simulation, Tool}
import tools.*
import tools.MultiSelectMode.{Closest2D, Closest3D, Heaviest, Largest}

import java.nio.file.{Path, Paths}
import scala.collection.mutable.Buffer
import scala.language.postfixOps
import scala.math.{cos, sin, sqrt}


object GUI extends JFXApp3:

  def start() =

    val currentDir: Path = Paths.get("").toAbsolutePath
    val defaultFile: Path = currentDir.resolve("solarsystem.json")


    var sim = Simulation()
    val tabSimulations: Buffer[Simulation] = Buffer()

    var bodyPanel: Option[BodyPanel] = None

    var cursorPixelPosition: Vector3D = Vector3D(0.0, 0.0)
    var cursorDragEnterPixelPosition: Option[Vector3D] = None
    var selectedOrbit: Option[Orbit] = None

    def bodyDrawRadius(body: Body): Double =
      math.max(body.radius / sim.metersPerPixel, 3)

    def bodiesVisible: Array[Body] =
      sim.space.bodies.filter(b =>
        val pos = b.position
        val minBound = sim.vPixelToPosition(Vector3D(0, 0))
        val maxBound = sim.vPixelToPosition(Vector3D(sim.canvas.width.value, sim.canvas.height.value))
        minBound.x < pos.x && pos.x < maxBound.x && minBound.y < pos.y && pos.y < maxBound.y
      ).toArray

    def fitZoom() =
      if sim.space.bodies.length >= 2 then
        val halfHeightZoomedAU = sim.space.bodies.map(body => (body.position - sim.centeringPosition).norm).max / Settings.metersPerAU
        val halfHeightAU = sim.canvas.height.value / 200
        sim.setZoom(halfHeightAU / halfHeightZoomedAU)
      else sim.setZoom(1.0)

    def focusZoom(body: Body) =
      if sim.space.bodies.length >= 2 then
        val halfWidthZoomedAU = sim.space.bodies
          .filter(_ != sim.selectedBody.getOrElse(sim.space.bodies.head))
          .map(body => (body.position - sim.centeringPosition).norm).min / Settings.metersPerAU
        val halfWidthAU = sim.canvas.width.value / 200
        sim.setZoom(halfWidthAU / halfWidthZoomedAU)
      else sim.setZoom(1.0)

    def vVector(body: Body) = body.velocity.unit / (bodiesVisible.maxBy(_.velocity.norm).velocity.norm / body.velocity.norm)

    def aVector(body: Body) = body.acceleration.unit / (bodiesVisible.maxBy(_.acceleration.norm).acceleration.norm / body.acceleration.norm)

    def center() =
      sim.centering = Centering.MassCenter
      fitZoom()

    def focus() =
      if sim.space.bodies.nonEmpty then
        val selection = sim.selectedBody.getOrElse(sim.space.bodies.head)
        sim.centering = Centering.AtBody(selection)
        focusZoom(selection)

    def moveCamera() =
      sim.centering = Centering.Custom(sim.cursorPosition)

    def saveAs() =
      val filechooser = new FileChooser:
        title = "Select a JSON simulation file"
        initialDirectory = currentDir.toFile
        extensionFilters.add(ExtensionFilter("Simulation files", "*.orbsm"))
      val file = filechooser.showSaveDialog(stage)
      if file != null then
        FileHandler.save(file.toString, sim)
        sim.workingFile = file.toString

    var ctrlPressed = false
    var shiftPressed = false

    stage = new JFXApp3.PrimaryStage:
      title = "Orbital Mechanics Simulator"
      width = 1920
      height = 1080
      fullScreen = true

    val root = new BorderPane()

    val sidePanel = new VBox:
      prefWidth = 310
      margin = Insets(20, 20, 20, 20)
      spacing = 0

    val sideScrollable = new ScrollPane:
      padding = Insets(20, 20, 20, 20)
      content = sidePanel
      hbarPolicy = ScrollBarPolicy.Never

    val bodyPanelContainer = new VBox()
    val simPanelContainer = new VBox()

    val spaceView = new StackPane()

    simPanelContainer.children = SimPanel(sim)

    val tabPane = new TabPane

    def simToTab(s: Simulation) =
      val tab = new Tab:
        text = s.name
        content = s.canvas
        sim = s
        onClosed = (event) => {
          tabSimulations.remove(tabSimulations.indexOf(s))
        }
        onSelectionChanged = (event) => {
          if this.selected.value then
            sim = s
            simPanelContainer.children = SimPanel(s)
        }
      tab

    def getTab(s: Simulation) =
      if tabPane.tabs.nonEmpty then
        tabPane.tabs(tabSimulations.indexOf(s))
      else
        javafx.scene.control.Tab()

    tabPane.tabs = tabSimulations.map(simToTab(_)).toSeq

    def selectBody(body: Body) =
      sim.select(body)
      bodyPanel = Some(BodyPanel(body))
      bodyPanelContainer.children = bodyPanel

    def initCanvas(): Canvas =
      var canvas1 = new Canvas():

        // scroll to zoom
        onScroll = (event) =>
          if event.getDeltaY > 0 then sim.setZoom(1.10 * sim.zoom)
          else sim.setZoom(sim.zoom / 1.10)

        // mouse click
        onMouseClicked = (event) =>
          event.getButton.toString match
            case "PRIMARY" =>
              val clickPosition = sim.pixelToPosition(event.getX, event.getY)

              // create a free body
              if Settings.tool == Tool.FreeBody && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                Settings.tool = Tool.Nothing
                sim.space.addBody(body)
                if BodyDialog(stage, body).process(sim) then
                  selectBody(body)


              // create an auto orbit
              else if Settings.tool == Tool.AutoOrbit && selectedOrbit.isDefined && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                body.velocity = selectedOrbit.get.velocity * (if shiftPressed then -1 else 1)
                sim.space.addBody(body)
                Settings.tool = Tool.Nothing
                if BodyDialog(stage, body).process(sim) then
                  selectBody(body)
                else
                  sim.space.removeBody(body)

              else
                if sim.selectableBody.isDefined then selectBody(sim.selectableBody.get)

            case "SECONDARY" =>
              sim.selectableBody match
                case Some(selection) =>
                  sim.pause()
                  BodyDialog(stage, selection).showAndWait()
                  bodyPanel.get.fullUpdate()
                  sim.play()
                case None => ()

        onMouseMoved = (event) =>
          cursorPixelPosition = Vector3D(event.getX, event.getY)

        onMouseDragged = (event) =>
          cursorPixelPosition = Vector3D(event.getX, event.getY)

        // start drag on canvas
        onMousePressed = (event) =>
          event.getButton.toString match
            case "PRIMARY" =>
              cursorDragEnterPixelPosition = Some(Vector3D(event.getX, event.getY))
              sim.centeringWhenEnteredDrag = Some(sim.centeringPosition)
            case _ => ()

        // stop drag on canvas
        onMouseReleased = (event) =>
          event.getButton.toString match
            case "PRIMARY" =>
              if (cursorDragEnterPixelPosition.getOrElse(Vector3D(0.0, 0.0)) - cursorPixelPosition).norm < 10 then deselectBody()
              cursorDragEnterPixelPosition = None
              sim.centeringWhenEnteredDrag = None
            case _ => ()
      canvas1.width <== spaceView.width
      canvas1.height <== spaceView.height
      canvas1

    def switchSim(newSim: Simulation) =
      if tabSimulations.contains(newSim) then
        tabPane.selectionModel().select(tabSimulations.indexOf(newSim))
        newSim.canvas = initCanvas()
      else
        newSim.canvas = initCanvas()
        tabSimulations += newSim
        tabPane.tabs += simToTab(newSim)
        tabPane.selectionModel().select(tabPane.tabs.length - 1)

      sim = newSim
      simPanelContainer.children = SimPanel(newSim)

    def deselectBody() =
      sim.deselect()
      bodyPanel = None
      bodyPanelContainer.children.clear()

    def save() =
      try
        FileHandler.save(sim.workingFile, sim)
      catch
        case _ => saveAs()

    def load() =
      val filechooser = new FileChooser:
        title = "Select a JSON simulation file"
        initialDirectory = currentDir.toFile
        extensionFilters.add(ExtensionFilter("Simulation files", "*.orbsm"))
      val file = filechooser.showOpenDialog(stage)
      if file != null then
        switchSim( FileHandler.load(file.toString) )
        sim.workingFile = file.toString

    val menuBar = new MenuBar:

        val menuFile = new Menu("File"):
          val itemNew = new MenuItem("New empty simulation"):
            onAction = (event) =>
              switchSim(Simulation())

          val itemSave = new MenuItem("Save"):
            onAction = (event) =>
              save()
          val itemSaveAs = new MenuItem("Save as..."):
            onAction = (event) =>
              saveAs()
          val itemOpen = new MenuItem("Open..."):
            onAction = (event) =>
              load()
          items = List(itemNew, itemSave, itemSaveAs, itemOpen)

        val menuSim = new Menu("Simulation"):
          val itemStart = new MenuItem("Start/Stop \t\t\t Ctrl+SPACE"):
            onAction = (event) =>
              sim.stopped = !sim.stopped
          val itemReverse = new MenuItem("Reverse time \t\t Ctrl+SPACE"):
            onAction = (event) =>
              sim.reversed = !sim.reversed
          val itemSpeedUp = new MenuItem("Speed x2 \t\t\t Ctrl+RIGHT"):
            onAction = (event) =>
              sim.setSpeed(sim.speed * 2)
          val itemSlowDown = new MenuItem("Speed x0.5 \t\t\t Ctrl+LEFT"):
            onAction = (event) =>
              sim.setSpeed(sim.speed / 2)
          items = List(itemStart, itemReverse, itemSpeedUp, itemSlowDown)

        val menuView = new Menu("View"):
          val itemCenter = new MenuItem("Center \t\t\t Ctrl+G"):
            onAction = (event) =>
              center()
          val itemFocus = new MenuItem("Focus \t\t\t Ctrl+F"):
            onAction = (event) =>
              focus()
          val itemResetZoom = new MenuItem("Reset zoom \t\t Ctrl+R"):
            onAction = (event) =>
              sim.setZoom(1.0)
          val itemZoomIn = new MenuItem("Zoom in \t\t\t Ctrl +"):
            onAction = (event) =>
              sim.setZoom(2 * sim.targetZoom)
          val itemZoomOut = new MenuItem("Zoom out \t\t Ctrl -"):
            onAction = (event) =>
              sim.setZoom(0.5 * sim.targetZoom)
          items = List(itemCenter, itemFocus, itemResetZoom, itemZoomIn, itemZoomOut)

        menus = List(menuFile, menuSim, menuView)

    sim.canvas = initCanvas()

    spaceView.children += tabPane

    // empty side panel
    sidePanel.children.clear()
    val toolBar = MainToolBar()

    val centerButton = new Button():
      tooltip = toolBar.barTooltip("Center")
      graphic = Icons.get("focus-mode")
      onAction = (event =>
        center()
      )

    val focusButton = new Button():
      tooltip = toolBar.barTooltip("Focus at the selected body")
      graphic = Icons.get("focus-3")
      onAction = (event =>
        focus()
      )

    val runSelector = ToggleGroup()

    val forwardSelector = new ToggleButton():
      tooltip = toolBar.barTooltip("Run")
      graphic = Icons.get("play")
      selected = true
      toggleGroup = runSelector
      onAction = (event) => { sim.stopped = false; sim.reversed = false }

    val stopSelector = new ToggleButton():
      tooltip = toolBar.barTooltip("Pause")
      graphic = Icons.get("pause")
      toggleGroup = runSelector
      onAction = (event) => sim.stopped = true

    val reverseSelector = new ToggleButton():
      tooltip = toolBar.barTooltip("Run (reverse time)")
      graphic = Icons.get("play-reverse")
      toggleGroup = runSelector
      onAction = (event) => { sim.stopped = false; sim.reversed = true }

    val speedUpButton = new Button():
      graphic = Icons.get("speed")
      onAction = (event) => { sim.targetSpeed *= 1.4 }

    val slowDownButton = new Button():
      graphic = Icons.get("rewind")
      onAction = (event) => { sim.targetSpeed *= .7143 }

    val speedLabel = new Label():
      font = Settings.fontMono
      alignment = Pos.Center
      text = sim.speed.toString + " days/s"
      prefWidth = 100

    def sep = new Separator():
      prefWidth = 70
      visible = false

    def minisep = new Separator():
      prefWidth = 10
      visible = false


    toolBar.items.add(centerButton)
    toolBar.items.add(focusButton)
    toolBar.items.add(sep)
    toolBar.items.add(reverseSelector)
    toolBar.items.add(stopSelector)
    toolBar.items.add(forwardSelector)
    toolBar.items.add(sep)
    toolBar.items.add(slowDownButton)
    toolBar.items.add(speedLabel)
    toolBar.items.add(speedUpButton)

    sidePanel.children = Array(bodyPanelContainer, simPanelContainer)

    // add child components
    root.top = menuBar
    root.right = sideScrollable
    root.center = spaceView
    root.bottom = toolBar

    // Define grid row and column size
    val column0 = new ColumnConstraints:
      percentWidth = 84
    val column1 = new ColumnConstraints:
      percentWidth = 16
    val row0 = new RowConstraints:
      percentHeight = 2
    val row1 = new RowConstraints:
      percentHeight = 98

    val scene = Scene(parent = root)

    stage.scene = scene

    // set dark mode by default
    scene.setUserAgentStylesheet("styles/cupertino-dark.css")
    Settings.theme = "/styles/cupertino-dark.css"

    // key press
    root.onKeyPressed = (event) =>
      event.getCode.toString match
        case "CONTROL" => ctrlPressed = true
        case "SHIFT" => shiftPressed = true
        case c => ()

    // key release
    root.onKeyReleased = (event) =>
      event.getCode.toString match
        case "Q" if ctrlPressed => Settings.tool = Tool.FreeBody                                                   // free body tool
        case "A" if ctrlPressed => Settings.tool = Tool.AutoOrbit                                                  // auto-orbit tool
        case "Z" if ctrlPressed => Settings.tool = Tool.Nothing                                                    // "nothing" tool

        case "SPACE" if ctrlPressed => sim.stopped = !sim.stopped                                             // start/stop

        case "LEFT" if ctrlPressed => sim.setSpeed(sim.speed / 2)                                                     // slow down 0.5x
        case "RIGHT" if ctrlPressed => sim.setSpeed(sim.speed * 2)                                                         // speed up 2x

        case "R" if ctrlPressed => sim.setZoom(1.0)                                                       // reset zoom
        case "EQUALS" if ctrlPressed => sim.setZoom(2 * sim.targetZoom)                                                   // zoom 2x
        case "MINUS" if ctrlPressed => sim.setZoom(0.5 * sim.targetZoom)                                                  // zoom 0.5x

        case "G" if ctrlPressed => center()                                                                   // center at mass center
        case "V" if ctrlPressed => moveCamera()                                                               // center at cursor position
        case "F" if sim.selectedBody.isDefined && ctrlPressed => focus()                                      // center at selection
        case "BACK_SPACE" if sim.selectedBody.isDefined && ctrlPressed => sim.deleteSelection()               // remove selection

        case "F11" => stage.fullScreen = !stage.isFullScreen

        case "CONTROL" => ctrlPressed = false
        case "SHIFT" => shiftPressed = false
        case c => ()

    try
      switchSim(FileHandler.load(defaultFile.toString))
    catch case _ => switchSim(Simulation())

    var simDT = 1.0
    var lastFPSDisplay = 0L
    var infoLabelText = ""

    val timer = AnimationTimer { now =>
      try {
      if sim.canvas.width.value != 0 && sim.canvas.height.value != 0 then {
      if tabSimulations.isEmpty then switchSim(Simulation())
      for tabSim <- tabSimulations do
        val gc = tabSim.canvas.graphicsContext2D

        // calculate dt and tick
        val deltaTime = (now - tabSim.lastFrame) / 1e9
        if deltaTime < 1 then tabSim.tick(deltaTime)
        if tabSim == sim then
          simDT = deltaTime
        tabSim.lastFrame = now

      val simGC = sim.canvas.graphicsContext2D

      // calculate the current cursor position
      sim.cursorPosition = sim.pixelToPosition(cursorPixelPosition.x, cursorPixelPosition.y)

      // calculate drag
      if Settings.tool == Nothing && sim.selectableBody.isEmpty then
        cursorDragEnterPixelPosition match
          case Some(enterPixelPosition: Vector3D) =>
            simGC.stroke = White
            simGC.lineWidth = 5
            simGC.beginPath()
            simGC.moveTo(enterPixelPosition.x, enterPixelPosition.y)
            simGC.lineTo(cursorPixelPosition.x, cursorPixelPosition.y)
            simGC.stroke()
            sim.centering = Centering.Custom(sim.centeringWhenEnteredDrag.get - (sim.cursorPosition - sim.pixelToPosition(enterPixelPosition.x, enterPixelPosition.y)))
          case None => ()

      // empty space
      simGC.fill = Purple
      simGC.fillRect(0, 0, sim.canvas.width.value, sim.canvas.height.value)

      // observable universe
      simGC.fill = Black
      val universeTopLeft = sim.positionToPixel(Vector3D(-2.909e+15 * Settings.metersPerAU, -2.909e+15 * Settings.metersPerAU))
      val universeDiameter = 2 * 2.909e+15 * Settings.metersPerAU
      simGC.fillOval(universeTopLeft.x, universeTopLeft.y, universeDiameter, universeDiameter)

      // smooth camera movement
      val oldZoom = sim.zoom
      sim.zoom += 0.05 * (sim.targetZoom - sim.zoom)                               // zoom change animation
      val correction = ((sim.cameraVelocity + sim.cameraAcceleration * simDT / 2) * (oldZoom / sim.zoom) * (sim.speed) *
        simDT * 86400 * (if sim.stopped then 0 else 1)) / sim.metersPerPixel
      sim.pixelOffset /= (oldZoom / sim.zoom)                                      // fixed: zoom display bug
      sim.pixelOffset += correction                                                // fixed: moving camera offset bug
      sim.pixelOffset += (sim.targetPixelOffset - sim.pixelOffset) * 0.05          // camera position animation



      // draw grid

      simGC.stroke = Color.rgb(30, 30, 30)
      simGC.lineWidth = 1
      val w = sim.canvas.width.value
      val h = sim.canvas.height.value
      var cellW = sim.pixelsPerAU
      val cellsPerScreenW = w / cellW
      val cellsPerScreenH = h / cellW

      val AUperCell = math.pow(10, math.floor(math.log10(cellsPerScreenW) - 0.8))  // scale, AU per cell
      cellW *= AUperCell

      val leftBound = - sim.pixelOffset.x + 0.5 * w
      val x0 = leftBound % cellW
      val upperBound = - sim.pixelOffset.y + 0.5 * h
      val y0 = upperBound % cellW

      if Settings.showGrid then
        for
          i <- 0 to (cellsPerScreenW / AUperCell).toInt
          j <- 0 to (cellsPerScreenH / AUperCell).toInt
        do
          simGC.strokeLine(x0 + i * cellW, 0, x0 + i * cellW, h)
          simGC.strokeLine(0, y0 + j * cellW, w, y0 + j * cellW)

      // draw the tool-specific visuals

      def dot(color: Color, coord: Vector3D) =
        simGC.fill = color
        simGC.fillOval(
          coord.x - 5,
          coord.y - 5,
          10,
          10
        )

      simGC.stroke = Gray
      simGC.lineWidth = 2
      Settings.tool match
        case Tool.FreeBody =>    // free body cross
          simGC.beginPath()
          simGC.moveTo(cursorPixelPosition.x, 0)
          simGC.lineTo(cursorPixelPosition.x, sim.canvas.height.value)
          simGC.stroke()

          simGC.beginPath()
          simGC.moveTo(0, cursorPixelPosition.y)
          simGC.lineTo(sim.canvas.width.value, cursorPixelPosition.y)
          simGC.stroke()

        case Tool.AutoOrbit if sim.selectedBody.isDefined =>    // auto-orbit ellipse
          val st = sim.space.attractorStrength(sim.selectedBody.get, sim.vPixelToPosition(cursorPixelPosition))
          simGC.stroke = if st > 0.9 || sim.stopped then Gray else Red
          val parentPosition = sim.selectedBody.get.position
          val baseDistance = (parentPosition - sim.vPixelToPosition(cursorDragEnterPixelPosition.getOrElse(cursorPixelPosition))).norm
          val startPosition = sim.vPixelToPosition(cursorPixelPosition)
          selectedOrbit = Some(Orbit(sim, baseDistance, startPosition, sim.selectedBody.get))
          selectedOrbit.get.draw()

        case _ =>
          ()

      // update the orbit precisions and draw the trails
      sim.space.bodies.foreach(body =>

        val orbitRadius = body.pathCurvatureRadius
        val orbitLength = orbitRadius * 2 * Settings.pi
        val fragmentLength = orbitLength / 256
        val fragmentTime = fragmentLength / body.velocity.norm
        val fragmentTicks = fragmentTime / sim.safeTimeStep
        body.setOrbitPrecision((fragmentTicks * 4/3).toInt)

        if Settings.showTrails then
          val drawRadius = bodyDrawRadius(body)
          simGC.lineWidth = 1
          var pos = sim.positionToPixel(body.position)
          for i <- 1 to 255 do
            if body.positionHistory.length > i then
              val posNew: Vector3D = sim.positionToPixel(body.positionHistory(i))
              simGC.stroke = body.color.interpolate(Transparent, 1 - i.toDouble / math.min(body.positionHistory.length, 255.0))

              simGC.beginPath()
              simGC.moveTo(pos.x, pos.y)
              simGC.lineTo(posNew.x, posNew.y)
              simGC.stroke()
              pos = posNew

          simGC.stroke = body.color
          pos = sim.positionToPixel(body.position)
          simGC.beginPath()
          simGC.moveTo(pos.x, pos.y)
          pos = sim.positionToPixel(body.positionHistory.last)
          simGC.lineTo(pos.x, pos.y)
          simGC.stroke()
      )


      // draw bodies and vectors
      sim.selectableBody = None
      bodiesVisible
        .sortWith((b1, b2) =>
          if b1.position.z < b2.position.z then
            true
          else if b1.position.z == b2.position.z then
            if b1.radius < b2.radius then
              true
            else if b1.radius == b2.radius then
              b1.mass < b2.mass
            else
              false
          else
            false
        )
        .foreach(body =>
          val drawRadius = bodyDrawRadius(body)
          var pos = sim.positionToPixel(body.position)

          // vectors
          simGC.lineWidth = 1
          if Settings.showVelocityVectors then
            val v = vVector(body) * (drawRadius + 20)
            simGC.stroke = LightGreen
            simGC.strokeLine(pos.x, pos.y, pos.x + v.x, pos.y + v.y)
          if Settings.showAccelerationVectors then
            val a = aVector(body) * (drawRadius + 20)
            simGC.stroke = Magenta
            simGC.strokeLine(pos.x, pos.y, pos.x + a.x, pos.y + a.y)

          // circle
          simGC.fill = body.color
          simGC.fillOval(
            pos.x - drawRadius,
            pos.y - drawRadius,
            drawRadius * 2,
            drawRadius * 2
          )

          // mark a body as selectable (if there are multiple, choose by multiselection mode)
          def distance(b: Body) = (cursorPixelPosition - sim.positionToPixel(b.position))
          val d = distance(body).noz.norm
          if d <= drawRadius + 0.01 * sim.canvas.width.value then
            sim.selectableBody match
              case Some(prevBody) =>
                if
                  Settings.multiSelectMode match
                    case Heaviest => prevBody.mass < body.mass
                    case Largest => prevBody.radius < body.radius
                    case Closest2D => d < distance(prevBody).noz.norm
                    case Closest3D => distance(body).norm < distance(prevBody).norm
                then sim.selectableBody = Some(body)
              case None => sim.selectableBody = Some(body)
        )

      // outline the selectable body and write its name
      sim.selectableBody.foreach( selection =>
        val pos = sim.positionToPixel(selection.position)
        simGC.stroke = Gray
        simGC.fill = selection.color
        val drawRadius = bodyDrawRadius(selection)
        simGC.strokeRoundRect(
          pos.x - drawRadius - 10,
          pos.y - drawRadius - 10,
          20 + 2 * drawRadius,
          20 + 2 * drawRadius,
          10,
          10
        )
        simGC.fillText(selection.name, pos.x - drawRadius - 10, pos.y - drawRadius - 15)
      )

      // outline the selected body
      simGC.stroke = Red
      sim.selectedBody match
        case Some(body) =>
          if !sim.space.bodies.contains(body) then
            deselectBody()
            if sim.centering == AtBody(body) then
              sim.centering = Centering.Custom(body.position)
            end if
          else
            val pos = sim.positionToPixel(body.position)
            val drawRadius = bodyDrawRadius(body)
            simGC.strokeRoundRect(
            pos.x - drawRadius - 10,
            pos.y - drawRadius - 10,
            20 + 2 * drawRadius,
            20 + 2 * drawRadius,
            10,
            10
            )
        case None => ()

      // update the GUI
      toolBar.update()
      speedLabel.text = sim.speed match
        case s if s < 1/1440.0 => f"${(sim.speed * 86400)}%.2f s/s"
        case s if s < 1/24.0 => f"${(sim.speed * 1440)}%.2f min/s"
        case s if s < 1 => f"${(sim.speed * 24)}%.2f h/s"
        case s if s < 365 => f"${(sim.speed)}%.2f days/s"
        case s => f"${(sim.speed / 365)}%.2f y/s"
      if sim.getAverageFPS < sim.fps then speedLabel.textFill = Color.Red else speedLabel.textFill = Color.White
      runSelector.selectToggle(if sim.stopped then stopSelector else if sim.reversed then reverseSelector else forwardSelector)
      if Settings.tool != AutoOrbit || sim.selectedBody.isEmpty then selectedOrbit = None

      if sim.selectedBody.isEmpty then
        bodyPanel = None
        bodyPanelContainer.children = List()
      if sim.selectedBody.isDefined && bodyPanelContainer.children.isEmpty then
        bodyPanel = Some(BodyPanel(sim.selectedBody.get))
        bodyPanelContainer.children = List(bodyPanel.get)
      if sim.selectedBody.isDefined && bodyPanel.isDefined then
        bodyPanel.get.update(sim)
      val tab = getTab(sim)
      if tab.getText != sim.name then tab.setText(sim.name)


      // draw the scale reference
      if Settings.showGrid then
        simGC.fill = White
        simGC.font = Settings.fontMono
        val label = Settings.formatScale(AUperCell)
        simGC.fillText(label, 20, h - 70)
        simGC.stroke = White
        simGC.lineWidth = 2
        simGC.strokeLine(20, h - 55, 20 + cellW, h - 55)

      // display FPS
      if Settings.showInfo then
        if now - lastFPSDisplay > 20000000 then
          lastFPSDisplay = now
          val avg = sim.getAverageFPS
          infoLabelText = f"${(avg).round.toString} FPS"

        simGC.fill = White
        sim.recordFPS(1.0 / simDT)
        simGC.font = Settings.fontMono
        simGC.fillText(
          infoLabelText,
          20, 30
        )

        if sim.getAverageFPS < 120 then
          simGC.fill = Yellow
          simGC.fillText(
          "\nOptimizing...",
          20, 30
          )

        if ctrlPressed then
          simGC.textAlign = TextAlignment.Right
          simGC.fill = White
          simGC.fillText(Settings.ctrlHelp(sim), sim.canvas.width.value - 20, 30)
          simGC.textAlign = TextAlignment.Left

        if sim.selectedBody.isDefined then
          simGC.fillText(f"\n${sim.selectedBody.get}", 20, 30)

        if selectedOrbit.isDefined then
          simGC.fillText(f"\n\n\n\n\n\n\n\n\n${selectedOrbit.get}", 20, 30)

        simGC.font = Settings.fontMain

      // alerts
      val alerts = AlertManager.get()
      for i <- alerts.indices do
        val alert = alerts(i)
        simGC.fill = jfxColor2sfx(alert.color.opacity(alert.opacity))
        simGC.fillRoundRect(sim.canvas.width.value - 420 + alert.shift, sim.canvas.height.value - 40 - (i * 60) - toolBar.height.value, 400, 40, 20, 20)
        simGC.fill = jfxColor2sfx(Color.White.opacity(alert.opacity))
        simGC.font = Settings.fontMono
        simGC.fillText(alert.message, sim.canvas.width.value - 400 + alert.shift, sim.canvas.height.value - 15 - (i * 60) - toolBar.height.value, 360)
     }}
      catch
        case e => AlertManager.alert(e.toString)
    }

    timer.start()

  end start

end GUI