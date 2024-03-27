import tools.Centering.{AtBody, MassCenter}
import tools.Tool.*
import engine.{Body, GravitationalForce, Vector3D}
import gui.AlertManager.stage
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
import scalafx.scene.text.Font
import scalafx.stage.{FileChooser, Stage, StageStyle}
import scalafx.stage.FileChooser.ExtensionFilter
import tools.{Centering, Simulation, Tool}
import tools.*
import tools.MultiSelectMode.{Closest2D, Closest3D, Heaviest, Largest}

import scala.collection.mutable.Buffer
import scala.language.postfixOps


object GUI extends JFXApp3:

  def start() =

    var sim = Simulation()
    val tabSimulations: Buffer[Simulation] = Buffer()

    var bodyPanel: Option[BodyPanel] = None

    var cursorPixelPosition: Vector3D = Vector3D(0.0, 0.0)
    var cursorDragEnterPixelPosition: Option[Vector3D] = None
    var lightMode = true

    def pixelToPosition(pixelX: Double, pixelY: Double) =
      (Vector3D(pixelX - sim.canvas.width.value / 2, pixelY - sim.canvas.height.value / 2) + sim.pixelOffset) * sim.metersPerPixel

    def vPixelToPosition(pixel: Vector3D): Vector3D =
      pixelToPosition(pixel.x, pixel.y)

    def positionToPixel(position: Vector3D) =
        (Vector3D(position.x / sim.metersPerPixel + sim.canvas.width.value / 2, position.y / sim.metersPerPixel + sim.canvas.height.value / 2)) - sim.pixelOffset

    def bodyDrawRadius(body: Body): Double =
      math.max(body.radius / sim.metersPerPixel, 3)

    def bodiesVisible: Array[Body] =
      sim.space.bodies.filter(b =>
        val pos = b.position
        val minBound = vPixelToPosition(Vector3D(0, 0))
        val maxBound = vPixelToPosition(Vector3D(sim.canvas.width.value, sim.canvas.height.value))
        minBound.x < pos.x && pos.x < maxBound.x && minBound.y < pos.y && pos.y < maxBound.y
      ).toArray

    def fitZoom() =
      val halfHeightZoomedAU = sim.space.bodies.map(body => (body.position - sim.centeringPosition).norm).max / Settings.metersPerAU
      val halfHeightAU = sim.canvas.height.value / 200
      sim.targetZoom = halfHeightAU / halfHeightZoomedAU

    def focusZoom(body: Body) =
      val halfWidthZoomedAU = sim.space.bodies
        .filter(_ != sim.selectedBody.getOrElse(sim.space.bodies.head))
        .map(body => (body.position - sim.centeringPosition).norm).min / Settings.metersPerAU
      val halfWidthAU = sim.canvas.width.value / 200
      sim.targetZoom = halfWidthAU / halfWidthZoomedAU

    def vVector(body: Body) = body.velocity.unit / (bodiesVisible.maxBy(_.velocity.norm).velocity.norm / body.velocity.norm)

    def aVector(body: Body) = body.acceleration.unit / (bodiesVisible.maxBy(_.acceleration.norm).acceleration.norm / body.acceleration.norm)

    def center() =
      sim.centering = Centering.MassCenter
      fitZoom()

    def focus() =
      val selection = sim.selectedBody.getOrElse(sim.space.bodies.head)
      sim.centering = Centering.AtBody(selection)
      focusZoom(selection)

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
              val clickPosition = pixelToPosition(event.getX, event.getY)

              // create a free body
              if Settings.tool == Tool.FreeBody && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                if BodyDialog(stage, body).process(sim) then
                  sim.space.addBody(body)
                  selectBody(body)
                Settings.tool = Tool.Nothing

              // create an auto orbit
              else if Settings.tool == Tool.AutoOrbit && sim.selectedBody.isDefined && sim.selectableBody.isEmpty then
                val body = Body(clickPosition)
                body.mass = Settings.earthMass
                body.radius = Settings.earthRadius
                sim.space.addAutoOrbit(body, sim.selectedBody.get)
                if BodyDialog(stage, body).process(sim) then
                  selectBody(body)
                else
                  sim.space.removeBody(body)
                Settings.tool = Tool.Nothing

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
            case "PRIMARY" if Settings.tool == Tool.Nothing && sim.selectableBody.isEmpty =>
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
          val itemStart = new MenuItem("Start/Stop \t\t\t Ctrl+SPACE"):
            onAction = (event) =>
              sim.stopped = !sim.stopped
          val itemSpeedUp = new MenuItem("Speed x2 \t\t\t Ctrl+RIGHT"):
            onAction = (event) =>
              sim.speed *= 2
          val itemSlowDown = new MenuItem("Speed x0.5 \t\t\t Ctrl+LEFT"):
            onAction = (event) =>
              sim.speed /= 2
          val itemTickUp = new MenuItem("Tickrate x2 \t\t\t Ctrl+UP"):
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

    val centerButton = new Button():
      graphic = Icons.get("focus-mode")
      onAction = (event =>
        center()
      )

    val focusButton = new Button():
      graphic = Icons.get("focus-3")
      onAction = (event =>
        focus()
      )

    val toolBar = MainToolBar()
    toolBar.items.add(centerButton)
    toolBar.items.add(focusButton)

    sidePanel.children = Array(bodyPanelContainer, simPanelContainer)

    // add child components
    root.top = menuBar
    root.right = sideScrollable
    root.center = spaceView
    root.bottom = toolBar

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
        Settings.theme = "/styles/cupertino-light.css"
      else
        scene.setUserAgentStylesheet("styles/cupertino-dark.css")
        Settings.theme = "/styles/cupertino-dark.css"

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
        case "Q" if ctrlPressed => Settings.tool = Tool.FreeBody                                                   // free body tool
        case "A" if ctrlPressed => Settings.tool = Tool.AutoOrbit                                                  // auto-orbit tool
        case "Z" if ctrlPressed => Settings.tool = Tool.Nothing                                                    // "nothing" tool

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
      sim.cursorPosition = pixelToPosition(cursorPixelPosition.x, cursorPixelPosition.y)

      // calculate drag
      cursorDragEnterPixelPosition match
        case Some(enterPixelPosition: Vector3D) =>
          simGC.stroke = White
          simGC.lineWidth = 5
          simGC.beginPath()
          simGC.moveTo(enterPixelPosition.x, enterPixelPosition.y)
          simGC.lineTo(cursorPixelPosition.x, cursorPixelPosition.y)
          simGC.stroke()
          sim.centering = Centering.Custom(sim.centeringWhenEnteredDrag.get - (sim.cursorPosition - pixelToPosition(enterPixelPosition.x, enterPixelPosition.y)))
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

        case Tool.AutoOrbit if sim.selectedBody.isDefined =>    // auto-orbit circle
          val pos = positionToPixel(sim.selectedBody.get.position)
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
        body.setOrbitPrecision((orbitLengthPixel / (speedPixel) / (sim.speed * 86400)) * sim.tpf / 256)

        if Settings.showTrails then
          val drawRadius = bodyDrawRadius(body)
          simGC.lineWidth = 1
          var pos = positionToPixel(body.positionHistory.head)
          for i <- 1 to 255 do
            if body.positionHistory.length > i then
              val posNew: Vector3D = positionToPixel(body.positionHistory(i))
              simGC.stroke = body.color.interpolate(Transparent, 1 - i.toDouble / math.min(body.positionHistory.length, 255.0))

              simGC.beginPath()
              simGC.moveTo(pos.x, pos.y)
              simGC.lineTo(posNew.x, posNew.y)
              simGC.stroke()
              pos = posNew
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
          var pos = positionToPixel(body.position)

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
          def distance(b: Body) = (cursorPixelPosition - positionToPixel(b.position))
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
        val pos = positionToPixel(selection.position)
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
            val pos = positionToPixel(body.position)
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

      if sim.selectedBody.isEmpty then
        bodyPanel = None
        bodyPanelContainer.children = List()
      if sim.selectedBody.isDefined && bodyPanelContainer.children.isEmpty then
        bodyPanel = Some(BodyPanel(sim.selectedBody.get))
        bodyPanelContainer.children = List(bodyPanel.get)
      if sim.selectedBody.isDefined && bodyPanel.isDefined then
        bodyPanel.get.update()
      val tab = getTab(sim)
      if tab.getText != sim.name then tab.setText(sim.name)


      // draw the scale reference
      if Settings.showGrid then
        simGC.fill = White
        simGC.font = Settings.fontMono
        val label = AUperCell match
          case x if x > 9000000 => f"1e${math.log10(x).round} AU"
          case x if x > 0.9 => f"${x.toInt} AU"
          case x if x > 0.09 => f"$x AU"
          case x => f"${(x * 150000000).toInt} km"
        simGC.fillText(label, 20, h - 70)
        simGC.stroke = White
        simGC.lineWidth = 2
        simGC.strokeLine(20, h - 55, 20 + cellW, h - 55)

      // draw the FPS & Debug label
      if Settings.showInfo then
        simGC.fill = if lightMode then Black else White
        sim.recordFPS(1.0 / simDT)
        val avg = sim.getAverageFPS
        simGC.font = Settings.fontMono
        simGC.fillText(
          f"${(avg).round.toString} FPS | ${(1000 / avg)}%.2f ms | ${sim.speed}%.2f days/s | ${(sim.speed / avg * 24)}%.2f h/frame \n" +
          { if !sim.stopped then f"${((avg).round * sim.tpf).toString} TPS | ${(1000 / avg / sim.tpf)}%.2f ms | ${(sim.speed / avg * 24 * 60 / sim.tpf)}%.2f min/tick \n" else "0 TPS\n" } +
          f"${sim.integrator} | show_trails: ${Settings.showTrails} | show_v: ${Settings.showVelocityVectors} | show_a: ${Settings.showAccelerationVectors} | ${Settings.tool} | " +
            f"${
              sim.centering match
                case Centering.AtBody(body) => body.name
                case Centering.Custom(pos) =>
                  val posAU = pos / 149597870700.0
                  f"${posAU.x}%.4f  ${posAU.y}%.4f  AU"
                case _ => sim.centering.toString
            } centering | ${bodiesVisible.length} bodies visible" +
           f" | ${sim.zoom}%.2fx zoom" + { if sim.selectedBody.isDefined then s"${sim.selectedBody.get}" else "" },
          20, 20
        )
        simGC.font = Settings.fontMain

      // alerts
      Platform.runLater {
        val alert = AlertManager.get()
        alert match
          case Some(a) =>
            val stop = sim.stopped
            sim.pause()
            a.showAndWait()
            if !stop then sim.play()
          case None => ()
      }
     }
      catch
        case e: NotImplementedError => AlertManager.alert("This feature is not implemented yet")
        case e => throw e
    }

    timer.start()

  end start

end GUI