import tools.Centering.{AtBody, MassCenter}
import tools.Tool.*
import engine.{Body, GravitationalForce, Vector3D}
import gui.AlertManager.stage
import gui.{AlertManager, BodyPanel, SimPanel, BodyDialog}
import scalafx.animation.AnimationTimer
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.control.*
import scalafx.scene.paint.Color
import scalafx.stage.{FileChooser, StageStyle}
import scalafx.stage.FileChooser.ExtensionFilter
import tools.{Centering, Simulation, Tool}
import tools.*
import java.util.NoSuchElementException

import scala.collection.mutable.Buffer


object GUI extends JFXApp3:

  def start() =
    var sim = Simulation()
    val tabSimulations: Buffer[Simulation] = Buffer()
    
    var cursorPixelPosition: Vector3D = Vector3D(0.0, 0.0)
    var cursorDragEnterPixelPosition: Option[Vector3D] = None
    var lightMode = true

    def pixelToPosition(canvas: Canvas, pixelX: Double, pixelY: Double): Vector3D =
        (Vector3D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + sim.pixelOffset) * sim.metersPerPixel

    def positionToPixel(canvas: Canvas, position: Vector3D): Vector3D =
        (Vector3D(position.x / sim.metersPerPixel + canvas.width.value / 2, position.y / sim.metersPerPixel + canvas.height.value / 2)) - sim.pixelOffset

    def bodyDrawRadius(body: Body): Double =
      math.max(body.radius / sim.metersPerPixel, 3)

    def center() =
      sim.centering = Centering.MassCenter; sim.resetZoom()

    def focus() =
      sim.centering = Centering.AtBody(sim.selectedBody.getOrElse(sim.space.bodies.head))

    def moveCamera() =
      sim.centering = Centering.Custom(sim.cursorPosition)

    def saveAs() =
      val filechooser = new FileChooser:
        title = "Select a JSON simulation file"
        initialDirectory = java.io.File("./src/main/scala/examples")
        extensionFilters.add(ExtensionFilter("JSON", "*.json"))
      val file = filechooser.showOpenDialog(stage)
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
      prefWidth = 300
      margin = Insets(10, 0, 0, 10)
      spacing = 10

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
      bodyPanelContainer.children = BodyPanel(body)

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
              val clickPosition = pixelToPosition(this, event.getX, event.getY)

              // create a free body
              if sim.tool == Tool.FreeBody && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                if BodyDialog(stage, body).process(sim) then
                  sim.space.addBody(body)
                  selectBody(body)
                sim.tool = Tool.Nothing

              // create an auto orbit
              else if sim.tool == Tool.AutoOrbit && sim.selectedBody.isDefined && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                sim.space.addAutoOrbit(body, sim.selectedBody.get)
                if BodyDialog(stage, body).process(sim) then
                  selectBody(body)
                else
                  sim.space.removeBody(body)
                sim.tool = Tool.Nothing

              else
                if sim.selectableBody.isDefined then selectBody(sim.selectableBody.get)

            case "SECONDARY" =>
              sim.selectableBody match
                case Some(selection) =>
                  sim.pause()
                  BodyDialog(stage, selection).showAndWait()
                  sim.play()
                case None => ()




        onMouseMoved = (event) =>
          cursorPixelPosition = Vector3D(event.getX, event.getY)

        onMouseDragged = (event) =>
          cursorPixelPosition = Vector3D(event.getX, event.getY)

        // start drag on canvas
        onMousePressed = (event) =>
          event.getButton.toString match
            case "PRIMARY" if sim.tool == Tool.Nothing && sim.selectableBody.isEmpty =>
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
      bodyPanelContainer.children.clear()

    def save() =
      try
        FileHandler.save(sim.workingFile, sim)
      catch
        case _ => saveAs()

    def load() =
      val filechooser = new FileChooser:
        title = "Select a JSON simulation file"
        initialDirectory = java.io.File(".")
        extensionFilters.add(ExtensionFilter("JSON", "*.json"))
      val file = filechooser.showOpenDialog(stage)
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
          val itemStart = new MenuItem("Start/Stop \t\t Ctrl+SPACE"):
            onAction = (event) =>
              sim.stopped = !sim.stopped
          val itemSpeedUp = new MenuItem("Speed x2 \t\t\t Ctrl+RIGHT"):
            onAction = (event) =>
              sim.speed *= 2
          val itemSlowDown = new MenuItem("Speed x0.5 \t\t Ctrl+LEFT"):
            onAction = (event) =>
              sim.speed /= 2
          val itemTickUp = new MenuItem("Tickrate x2 \t\t Ctrl+UP"):
            onAction = (event) =>
              sim.setTPF(sim.tpf * 2)
          val itemTickDown = new MenuItem("Tickrate x0.5 \t\t Ctrl+DOWN"):
            onAction = (event) =>
              sim.setTPF(sim.tpf / 2)
          items = List(itemStart, itemSpeedUp, itemSlowDown, itemTickUp, itemTickDown)

        val menuView = new Menu("View"):
          val itemCenter = new MenuItem("Center \t\t\t Ctrl+G"):
            onAction = (event) =>
              center()
          val itemFocus = new MenuItem("Focus \t\t\t Ctrl+F"):
            onAction = (event) =>
              focus()
          val itemResetZoom = new MenuItem("Reset zoom \t\t Ctrl+R"):
            onAction = (event) =>
              sim.targetZoom = 1.0
          val itemZoomIn = new MenuItem("Zoom in \t\t\t Ctrl +"):
            onAction = (event) =>
              sim.targetZoom *= 2
          val itemZoomOut = new MenuItem("Zoom out \t\t Ctrl -"):
            onAction = (event) =>
              sim.targetZoom *= 0.5
          items = List(itemCenter, itemFocus, itemResetZoom, itemZoomIn, itemZoomOut)

        menus = List(menuFile, menuSim, menuView)


    sim.canvas = initCanvas()



    spaceView.children += tabPane

    // empty side panel
    sidePanel.children.clear()

    val toolSelector = ToggleGroup()

    val nothingSelector = new ToggleButton("Nothing"):
      toggleGroup = toolSelector
      selected = true
      onAction = (event) => sim.tool = Tool.Nothing

    val freeBodySelector = new ToggleButton("Free Body"):
      toggleGroup = toolSelector
      onAction = (event) => sim.tool = Tool.FreeBody

    val autoOrbitSelector = new ToggleButton("Auto Orbit"):
      toggleGroup = toolSelector
      onAction = (event) => sim.tool = Tool.AutoOrbit

    val toolBox = new HBox:
      spacing = 10
      children = Array(
        freeBodySelector,
        autoOrbitSelector,
        nothingSelector
      )

    sidePanel.children = Array(toolBox, bodyPanelContainer, simPanelContainer)

    // add child components
    root.top = menuBar
    root.right = sidePanel
    root.center = spaceView

    // set up alerts
    AlertManager.stage = Some(stage)

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

    def toggleLightMode() =
      lightMode = !lightMode
      if lightMode then
        scene.setUserAgentStylesheet("styles/cupertino-light.css")
      else
        scene.setUserAgentStylesheet("styles/cupertino-dark.css")

    // set dark mode by default
    toggleLightMode()

    // key press
    root.onKeyPressed = (event) =>
      event.getCode.toString match
        case "CONTROL" => ctrlPressed = true
        case "SHIFT" => shiftPressed = true
        case c => ()

    // key release
    root.onKeyReleased = (event) =>
      event.getCode.toString match
        case "Q" if ctrlPressed => sim.tool = Tool.FreeBody                                                   // free body tool
        case "A" if ctrlPressed => sim.tool = Tool.AutoOrbit                                                  // auto-orbit tool
        case "Z" if ctrlPressed => sim.tool = Tool.Nothing                                                    // "nothing" tool

        case "SPACE" if ctrlPressed => sim.stopped = !sim.stopped                                             // start/stop

        case "LEFT" if ctrlPressed => sim.speed /= 2                                                          // slow down 0.5x
        case "RIGHT" if ctrlPressed => sim.speed *= 2                                                         // speed up 2x
        case "DOWN" if ctrlPressed => sim.setTPF(sim.tpf / 2)                                                 // tickrate 0.5x
        case "UP" if ctrlPressed => sim.setTPF(sim.tpf * 2)                                                   // tickrate 2x

        case "R" if ctrlPressed => sim.targetZoom = 1.0                                                       // reset zoom
        case "EQUALS" if ctrlPressed => sim.targetZoom *= 2                                                   // zoom 2x
        case "MINUS" if ctrlPressed => sim.targetZoom *= 0.5                                                  // zoom 0.5x

        case "G" if ctrlPressed => center()                                                                   // center at mass center
        case "V" if ctrlPressed => moveCamera()                                                               // center at cursor position
        case "F" if sim.selectedBody.isDefined && ctrlPressed => focus()                                      // center at selection
        case "E" if ctrlPressed => AlertManager.alert("test")                                                 // test errors
        case "BACK_SPACE" if sim.selectedBody.isDefined && ctrlPressed => sim.deleteSelection()               // remove selection
        case "ESCAPE" => deselectBody()                                                                       // deselect

        case "F11" => stage.fullScreen = !stage.isFullScreen

        case "L" => toggleLightMode()                                                                         // light mode

        case "CONTROL" => ctrlPressed = false
        case "SHIFT" => shiftPressed = false
        case c => ()

    switchSim(FileHandler.load("./src/main/scala/examples/solarsystem.json"))

    var simDT = 1.0
    val timer = AnimationTimer { now =>
      try {
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
      sim.cursorPosition = pixelToPosition(sim.canvas, cursorPixelPosition.x, cursorPixelPosition.y)

      // calculate drag
      cursorDragEnterPixelPosition match
        case Some(enterPixelPosition: Vector3D) =>
          simGC.stroke = White
          simGC.lineWidth = 5
          simGC.beginPath()
          simGC.moveTo(enterPixelPosition.x, enterPixelPosition.y)
          simGC.lineTo(cursorPixelPosition.x, cursorPixelPosition.y)
          simGC.stroke()
          sim.centering = Centering.Custom(sim.centeringWhenEnteredDrag.get - (sim.cursorPosition - pixelToPosition(sim.canvas, enterPixelPosition.x, enterPixelPosition.y)))
        case None => ()

      // empty dark space
      simGC.fill = if lightMode then LightGray else Black
      simGC.fillRect(0, 0, sim.canvas.width.value, sim.canvas.height.value)

      // smooth camera movement
      val oldZoom = sim.zoom
      sim.zoom += 0.05 * (sim.targetZoom - sim.zoom)                               // zoom change animation
      val correction = ((sim.cameraVelocity + sim.cameraAcceleration * simDT / 2) * (oldZoom / sim.zoom) * (sim.speed) *
        simDT * 86400 * (if sim.stopped then 0 else 1)) / sim.metersPerPixel
      sim.pixelOffset /= (oldZoom / sim.zoom)                                      // fixed: zoom display bug
      sim.pixelOffset += correction                                                // fixed: moving camera offset bug
      val positionChange =
      sim.pixelOffset += (sim.targetPixelOffset - sim.pixelOffset) * 0.05          // camera position animation

      // val oldZoom = sim.zoom
      // val correction = ((sim.cameraVelocity ) * simDT
      //   * sim.speed * 86400 * (if sim.stopped then 0 else 1)) / sim.metersPerPixel               // new correction with acceleration
      // sim.zoom += 0.05 * (sim.targetZoom - sim.zoom)             // zoom change animation
      // sim.pixelOffset /= (oldZoom / sim.zoom)                        // zoom display bug fixed
      // sim.pixelOffset += correction                                  // moving camera offset bug (attempt to fix)
      // sim.pixelOffset += (sim.targetPixelOffset - sim.pixelOffset) * 0.05    // camera position animation
          
      // draw the tool-specific visuals
      simGC.stroke = Gray
      simGC.lineWidth = 2
      sim.tool match
        case Tool.FreeBody =>    // free body cross
          simGC.beginPath()
          simGC.moveTo(cursorPixelPosition.x, 0)
          simGC.lineTo(cursorPixelPosition.x, sim.canvas.height.value)
          simGC.stroke()

          simGC.beginPath()
          simGC.moveTo(0, cursorPixelPosition.y)
          simGC.lineTo(sim.canvas.width.value, cursorPixelPosition.y)
          simGC.stroke()

        case Tool.AutoOrbit if sim.selectedBody.isDefined =>    // auto-orbit circle
          val pos = positionToPixel(sim.canvas, sim.selectedBody.get.position)
          val distance = (pos - cursorPixelPosition).norm

          simGC.strokeOval(
            pos.x - distance,
            pos.y - distance,
            distance * 2,
            distance * 2
          )

        case _ =>
          ()

      // update the orbit precisions and draw the trails
      sim.space.bodies.foreach(body =>

        val orbitRadius = body.velocity.norm * body.velocity.norm / body.acceleration.norm
        val orbitLengthPixel = (orbitRadius * 2 * Settings.pi) / sim.metersPerPixel
        val speedPixel = body.velocity.norm / sim.metersPerPixel
        body.setOrbitPrecision((orbitLengthPixel / (speedPixel) / (sim.speed * 86400)) * sim.tpf)

        val drawRadius = bodyDrawRadius(body)
        simGC.lineWidth = 1
        var pos = positionToPixel(sim.canvas, body.positionHistory.head)
        for i <- 1 to 255 do
          if body.positionHistory.length > i then
            val posNew: Vector3D = positionToPixel(sim.canvas, body.positionHistory(i))
            simGC.stroke = body.color.interpolate(Black, 1 - i.toDouble / math.min(body.positionHistory.length, 255.0))

            simGC.beginPath()
            simGC.moveTo(pos.x, pos.y)
            simGC.lineTo(posNew.x, posNew.y)
            simGC.stroke()
            pos = posNew

      )

      // draw the bodies
      sim.selectableBody = None
      sim.space.bodies.foreach(body =>
        val drawRadius = bodyDrawRadius(body)
        var pos = positionToPixel(sim.canvas, body.position)
        simGC.fill = body.color
        simGC.fillOval(
          pos.x - drawRadius,
          pos.y - drawRadius,
          drawRadius * 2,
          drawRadius * 2
        )

        // mark a body as selectable (if there are multiple, choose the most massive)
        if (cursorPixelPosition - positionToPixel(sim.canvas, body.position)).norm <= drawRadius + 0.01 * sim.canvas.width.value then
          sim.selectableBody match
            case Some(prevBody) =>
              if prevBody.mass < body.mass then sim.selectableBody = Some(body)
            case None => sim.selectableBody = Some(body)

      )

      // outline the selectable body and write its name
      sim.selectableBody.foreach( selection =>
        val pos = positionToPixel(sim.canvas, selection.position)
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
            val pos = positionToPixel(sim.canvas, body.position)
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
      sim.tool match
        case Tool.Nothing => nothingSelector.selected = true
        case Tool.FreeBody => freeBodySelector.selected = true
        case Tool.AutoOrbit => autoOrbitSelector.selected = true
      if sim.selectedBody.isEmpty then
        bodyPanelContainer.children = List()
      if sim.selectedBody.nonEmpty && bodyPanelContainer.children.isEmpty then
        bodyPanelContainer.children = List(BodyPanel(sim.selectedBody.get))
      val tab = getTab(sim)
      if tab.getText != sim.name then tab.setText(sim.name)

      // draw the FPS & Debug label
      simGC.fill = if lightMode then Black else White
      sim.recordFPS(1.0 / simDT)
      val avg = sim.getAverageFPS
      simGC.fillText(
        f"${(avg).round.toString} FPS | ${(1000 / avg)}%.2f ms | ${sim.speed}%.2f days/s | ${(sim.speed / avg * 24)}%.2f h/frame \n" +
        { if !sim.stopped then f"${((avg).round * sim.tpf).toString} TPS | ${(1000 / avg / sim.tpf)}%.2f ms | ${(sim.speed / avg * 24 * 60 / sim.tpf)}%.2f min/tick \n" else "0 TPS\n" } +
        f"${sim.tool} | " +
          f"${
            sim.centering match
              case Centering.AtBody(body) => body.name
              case Centering.Custom(pos) =>
                val posAU = pos / 149597870700.0
                f"${posAU.x}%.4f  ${posAU.y}%.4f  AU"
              case _ => sim.centering.toString
          } centering" +
         f" | ${sim.zoom}%.2fx zoom" + { if sim.selectedBody.isDefined then s"${sim.selectedBody.get}" else "" },
        20, 20
      )

      // alerts
      Platform.runLater {
        val alert = AlertManager.get()
        alert match
          case Some(a) => a.showAndWait()
          case None => ()
      }
     }
     catch
       case e => AlertManager.alert(e.toString)
    }

    timer.start()

  end start

end GUI