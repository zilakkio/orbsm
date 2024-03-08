import tools.Centering.{AtBody, MassCenter}
import tools.Tool.*
import engine.{Body, GravitationalForce, Vector3D}
import gui.{AlertManager, BodyPanel, SimPanel}
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.control.*
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import tools.{Centering, Simulation, Tool}
import tools.*


object GUI extends JFXApp3:

  def start() =
    var sim = Simulation()

    {    // set up the default simulation
      val sun = Body(Vector3D(0.0, 0.0), name="Sun")
      sun.radius = 109.076 * 6371000.0
      sun.mass = 333030.262 * 5.9722e24
      sun.color = Yellow

      val earth = Body(Vector3D(149597870700.0, 0.0), name="Earth")
      earth.radius = 6371000.0
      earth.mass = 5.9722e24
      earth.velocity = Vector3D(0.0, -29780.0)
      earth.color = Blue

      val moon = Body(Vector3D(149984400000.0, 0.0), name="Moon")
      moon.radius = 0.2727 * 6371000.0
      moon.mass = 0.0123000371 * 5.9722e24
      moon.velocity = Vector3D(0.0, -28758.0)

      sim.space.addBody(sun)
      sim.space.addBody(moon)
      sim.space.addBody(earth)
      sim.space.interactionForces = Vector(GravitationalForce)
    }

    val minPixelsPerAU = 100.0
    val minMetersPerPixel = 1/minPixelsPerAU * 1.496e11

    def metersPerPixel = minMetersPerPixel / sim.zoom
    def targetPixelOffset = sim.centeringPosition / metersPerPixel

    var pixelOffset: Vector3D = targetPixelOffset

    var cursorPixelPosition: Vector3D = Vector3D(0.0, 0.0)
    var cursorPosition: Vector3D = Vector3D(0.0, 0.0)
    var selectableBody: Option[Body] = None

    var cursorDragEnterPixelPosition: Option[Vector3D] = None
    var centeringWhenEnteredDrag: Option[Vector3D] = None

    var lightMode = true

    def pixelToPosition(canvas: Canvas, pixelX: Double, pixelY: Double): Vector3D =
        (Vector3D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + pixelOffset) * metersPerPixel

    def positionToPixel(canvas: Canvas, position: Vector3D): Vector3D =
        (Vector3D(position.x / metersPerPixel + canvas.width.value / 2, position.y / metersPerPixel + canvas.height.value / 2)) - pixelOffset

    def bodyDrawRadius(body: Body): Double =
      math.max(body.radius / metersPerPixel, 3)

    def center() =
      sim.centering = Centering.MassCenter; sim.resetZoom()

    def focus() =
      sim.centering = Centering.AtBody(sim.selectedBody.getOrElse(sim.space.bodies.head))

    def moveCamera() =
      sim.centering = Centering.Custom(cursorPosition)

    def saveAs() =
      val filechooser = new FileChooser:
        title = "Select JSON simulation file"
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

    val root = new BorderPane()

    val sidePanel = new VBox:
      prefWidth = 300
      margin = Insets(10, 0, 0, 10)
      spacing = 10

    val bodyPanelContainer = new VBox()
    val simPanelContainer = new VBox()

    val spaceView = new Pane()

    simPanelContainer.children = SimPanel(sim)

    def selectBody(body: Body) =
      sim.select(body)
      bodyPanelContainer.children = BodyPanel(body)

    def switchSim(newSim: Simulation) =
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
        title = "Select JSON simulation file"
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
              val body = Body(Vector3D(0,0))
              body.mass = 5.9722e24
              body.radius = 6371000.0
              sim.space.addBody(body)

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


    val canvas = new Canvas():

      // scroll to zoom
      onScroll = (event) =>
        if event.getDeltaY > 0 then sim.setZoom(1.10 * sim.zoom)
        else sim.setZoom(sim.zoom / 1.10)

      // mouse click
      onMouseClicked = (event) =>
        val clickPosition = pixelToPosition(this, event.getX, event.getY)

        // create a free body
        if sim.tool == Tool.FreeBody && selectableBody.isEmpty then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addBody(body)
          selectBody(body)
          sim.tool = Tool.Nothing

        // create an auto orbit
        else if sim.tool == Tool.AutoOrbit && sim.selectedBody.isDefined && selectableBody.isEmpty then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addAutoOrbit(body, sim.selectedBody.get)
          selectBody(body)
          sim.tool = Tool.Nothing

        else
          if selectableBody.isDefined then selectBody(selectableBody.get)

      onMouseMoved = (event) =>
        cursorPixelPosition = Vector3D(event.getX, event.getY)

      onMouseDragged = (event) =>
        cursorPixelPosition = Vector3D(event.getX, event.getY)


    canvas.width <== spaceView.width
    canvas.height <== spaceView.height

    spaceView.children.add(canvas)

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

    // start drag on canvas
    canvas.onMousePressed = (event) =>
      event.getButton.toString match
        case "PRIMARY" if sim.tool == Tool.Nothing && selectableBody.isEmpty =>
          cursorDragEnterPixelPosition = Some(Vector3D(event.getX, event.getY))
          centeringWhenEnteredDrag = Some(sim.centeringPosition)
        case _ => ()

    // stop drag on canvas
    canvas.onMouseReleased = (event) =>
      event.getButton.toString match
        case "PRIMARY" =>
          if (cursorDragEnterPixelPosition.getOrElse(Vector3D(0.0, 0.0)) - cursorPixelPosition).norm < 10 then deselectBody()
          cursorDragEnterPixelPosition = None
          centeringWhenEnteredDrag = None
        case _ => ()

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

        case "L" => toggleLightMode()                                                                         // light mode

        case "CONTROL" => ctrlPressed = false
        case "SHIFT" => shiftPressed = false
        case c => ()

    var lastFrame = 0L

    val timer = AnimationTimer { now =>
        val gc = canvas.graphicsContext2D

        // calculate the current cursor position
        cursorPosition = pixelToPosition(canvas, cursorPixelPosition.x, cursorPixelPosition.y)

        // calculate drag
        cursorDragEnterPixelPosition match
          case Some(enterPixelPosition: Vector3D) =>
            gc.stroke = White
            gc.lineWidth = 5
            gc.beginPath()
            gc.moveTo(enterPixelPosition.x, enterPixelPosition.y)
            gc.lineTo(cursorPixelPosition.x, cursorPixelPosition.y)
            gc.stroke()
            sim.centering = Centering.Custom(centeringWhenEnteredDrag.get - (cursorPosition - pixelToPosition(canvas, enterPixelPosition.x, enterPixelPosition.y)))
          case None => ()

        // calculate dt
        val deltaTime = (now - lastFrame) / 1e9
        if deltaTime < 1 then sim.tick(deltaTime)
        lastFrame = now

        // smooth camera movement
        val oldZoom = sim.zoom
        val correction = (sim.cameraVelocity * deltaTime * sim.speed * 86400 * (if sim.stopped then 0 else 1)) / metersPerPixel
        sim.zoom += 0.05 * (sim.targetZoom - sim.zoom)             // zoom change animation
        pixelOffset /= (oldZoom / sim.zoom)                        // fixed: zoom display bug
        pixelOffset += correction                                  // fixed: moving camera offset bug
        pixelOffset += (targetPixelOffset - pixelOffset) * 0.05    // camera position animation

        // empty dark space
        gc.fill = if lightMode then LightGray else Black
        gc.fillRect(0, 0, canvas.width.value, canvas.height.value)

        // draw the tool-specific visuals
        sim.tool match
          case Tool.FreeBody =>    // free body cross
            gc.stroke = Gray

            gc.lineWidth = 2

            gc.beginPath()
            gc.moveTo(cursorPixelPosition.x, 0)
            gc.lineTo(cursorPixelPosition.x, canvas.height.value)
            gc.stroke()

            gc.beginPath()
            gc.moveTo(0, cursorPixelPosition.y)
            gc.lineTo(canvas.width.value, cursorPixelPosition.y)
            gc.stroke()

          case Tool.AutoOrbit if sim.selectedBody.isDefined =>    // auto-orbit circle
            gc.stroke = Gray

            gc.lineWidth = 2

            val pos = positionToPixel(canvas, sim.selectedBody.get.position)
            val distance = (pos - cursorPixelPosition).norm

            gc.strokeOval(
              pos.x - distance,
              pos.y - distance,
              distance * 2,
              distance * 2
            )

          case _ =>
            ()

        // draw the paths
        sim.space.bodies.foreach(body =>
          val drawRadius = bodyDrawRadius(body)
          gc.lineWidth = 1
          var pos = positionToPixel(canvas, body.positionHistory.head)
          for i <- 1 to 255 do
            if body.positionHistory.length > i then
              val posNew: Vector3D = positionToPixel(canvas, body.positionHistory(i))
              gc.stroke = body.color.interpolate(Black, 1 - i.toDouble / math.min(body.positionHistory.length, 255.0))

              gc.beginPath()
              gc.moveTo(pos.x, pos.y)
              gc.lineTo(posNew.x, posNew.y)
              gc.stroke()
              pos = posNew

        )

        // draw the bodies
        selectableBody = None
        sim.space.bodies.foreach(body =>
          val drawRadius = bodyDrawRadius(body)
          var pos = positionToPixel(canvas, body.position)
          gc.fill = body.color
          gc.fillOval(
            pos.x - drawRadius,
            pos.y - drawRadius,
            drawRadius * 2,
            drawRadius * 2
          )

          // mark a body as selectable (if there are multiple, choose the most massive)
          if (cursorPixelPosition - positionToPixel(canvas, body.position)).norm <= drawRadius + 0.01 * canvas.width.value then
            selectableBody match
              case Some(prevBody) =>
                if prevBody.mass < body.mass then selectableBody = Some(body)
              case None => selectableBody = Some(body)

        )

        // outline the selectable body and write its name
        selectableBody.foreach( selection =>
          val pos = positionToPixel(canvas, selection.position)
          gc.stroke = Gray
          gc.fill = selection.color
          val drawRadius = bodyDrawRadius(selection)
          gc.strokeRoundRect(
            pos.x - drawRadius - 10,
            pos.y - drawRadius - 10,
            20 + 2 * drawRadius,
            20 + 2 * drawRadius,
            10,
            10
          )
          gc.fillText(selection.name, pos.x - drawRadius - 10, pos.y - drawRadius - 15)
      )

        // outline the selected body
        gc.stroke = Red
        sim.selectedBody match
          case Some(body) =>
            if !sim.space.bodies.contains(body) then
              deselectBody()
              if sim.centering == AtBody(body) then
                sim.centering = Centering.Custom(body.position)
              end if
            else
              val pos = positionToPixel(canvas, body.position)
              val drawRadius = bodyDrawRadius(body)
              gc.strokeRoundRect(
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

        // draw the FPS & Debug label
        gc.fill = if lightMode then Black else White
        sim.recordFPS(1.0 / deltaTime)
        val avg = sim.getAverageFPS
        gc.fillText(
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
    }

    timer.start()

  end start

end GUI