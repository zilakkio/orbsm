import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.control.*
import scalafx.scene.paint.Color
import scalafx.scene.shape.Line

object GUI extends JFXApp3:

  def start() =
    var sim = Simulation()

    val minPixelsPerAU = 100.0
    val minMetersPerPixel = 1/minPixelsPerAU * 1.496e11

    var pixelOffset: Vector2D = Vector2D(0.0, 0.0)

    def metersPerPixel = minMetersPerPixel / sim.zoom
    def targetPixelOffset = sim.centeringPosition / metersPerPixel

    var cursorPixelPosition: Vector2D = Vector2D(0.0, 0.0)
    var cursorPosition: Vector2D = Vector2D(0.0, 0.0)
    var selectableBody: Option[Body] = None

    var cursorDragEnterPixelPosition: Option[Vector2D] = None
    var centeringWhenEnteredDrag: Option[Vector2D] = None

    def pixelToPosition(canvas: Canvas, pixelX: Double, pixelY: Double): Vector2D =
        (Vector2D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + pixelOffset) * metersPerPixel

    def positionToPixel(canvas: Canvas, position: Vector2D): Vector2D =
        (Vector2D(position.x / metersPerPixel + canvas.width.value / 2, position.y / metersPerPixel + canvas.height.value / 2)) - pixelOffset

    def recenterImmediately() =
      pixelOffset = targetPixelOffset

    val sun = Body(Vector2D(0.0, 0.0), name="Sun")
    sun.radius = 109.076 * 6371000.0
    sun.mass = 333030.262 * 5.9722e24
    sun.color = Yellow

    val earth = Body(Vector2D(149597870700.0, 0.0), name="Earth")
    earth.radius = 6371000.0
    earth.mass = 5.9722e24
    earth.velocity = Vector2D(0.0, -29780.0)
    earth.color = Blue

    val moon = Body(Vector2D(149984400000.0, 0.0), name="Moon")
    moon.radius = 0.2727 * 6371000.0
    moon.mass = 0.0123000371 * 5.9722e24
    moon.velocity = Vector2D(0.0, -28758.0)

    sim.space.addBody(sun)
    sim.space.addBody(moon)
    sim.space.addBody(earth)

    sim.select(earth)
    sim.space.interactionForces = Vector(GravitationalForce)

    stage = new JFXApp3.PrimaryStage:
      title = "Orbital Mechanics Simulator"
      width = 1920
      height = 1080

    val menuBar = new MenuBar:
        val menuFile = new Menu("File"):
          val itemNew = new MenuItem("New...")
          val itemSave = new MenuItem("Save")
          items = List(itemNew, itemSave)
        menus = List(menuFile)

    val root = GridPane()

    val sidePanel = new VBox:
      margin = Insets(10, 0, 0, 10)
      spacing = 10

    val spaceView = new Pane()

    val canvas = new Canvas():

      // scroll to zoom
      onScroll = (event) =>
        val oldZoom = sim.zoom
        if event.getDeltaY > 0 then sim.setZoom(1.10 * sim.zoom)
        else sim.setZoom(sim.zoom / 1.10)

      // mouse click
      onMouseClicked = (event) =>
        val clickPosition = pixelToPosition(this, event.getX, event.getY)

        // create a free body
        if sim.tool == Tool.FreeBody then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addBody(body)
          sim.select(body)
          sim.tool = Tool.Nothing

        // create an auto orbit
        else if sim.tool == Tool.AutoOrbit && sim.selectedBody.isDefined then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addAutoOrbit(body, sim.selectedBody.get)
          sim.select(body)
          sim.tool = Tool.Nothing

        else
          if selectableBody.isDefined then sim.select(selectableBody.get)

      onMouseMoved = (event) =>
        cursorPixelPosition = Vector2D(event.getX, event.getY)

      onMouseDragged = (event) =>
        cursorPixelPosition = Vector2D(event.getX, event.getY)


    canvas.width <== spaceView.width
    canvas.height <== spaceView.height

    spaceView.children.add(canvas)

    // empty side panel
    sidePanel.children.clear()

    val toolSelector = ToggleGroup()

    val nothingSelector = new RadioButton("Nothing"):
      toggleGroup = toolSelector
      selected = true
      onAction = (event) => sim.tool = Tool.Nothing

    val freeBodySelector = new RadioButton("Free Body"):
      toggleGroup = toolSelector
      onAction = (event) => sim.tool = Tool.FreeBody

    val autoOrbitSelector = new RadioButton("Auto Orbit"):
      toggleGroup = toolSelector
      onAction = (event) => sim.tool = Tool.AutoOrbit

    sidePanel.children = Array(
      nothingSelector,
      freeBodySelector,
      autoOrbitSelector
    )

    // Add child components to the grid
    root.add(menuBar, 0, 0)
    root.add(spaceView, 0, 1)
    root.add(sidePanel, 1, 0, 1, 2)

    // Define grid row and column size
    val column0 = new ColumnConstraints:
      percentWidth = 84
    val column1 = new ColumnConstraints:
      percentWidth = 16
    val row0 = new RowConstraints:
      percentHeight = 2
    val row1 = new RowConstraints:
      percentHeight = 98

    root.columnConstraints = Array(column0, column1) // Add constraints in order
    root.rowConstraints = Array(row0, row1)

    val scene = Scene(parent = root)

    stage.scene = scene

    // start drag on canvas
    canvas.onMousePressed = (event) =>
      event.getButton.toString match
        case "PRIMARY" if sim.tool == Tool.Nothing && selectableBody.isEmpty =>
          cursorDragEnterPixelPosition = Some(Vector2D(event.getX, event.getY))
          centeringWhenEnteredDrag = Some(sim.centeringPosition)
        case _ => ()

    // stop drag on canvas
    canvas.onMouseReleased = (event) =>
      event.getButton.toString match
        case "PRIMARY" =>
          if (cursorDragEnterPixelPosition.getOrElse(Vector2D(0.0, 0.0)) - cursorPixelPosition).norm < 10 then sim.deselect()
          cursorDragEnterPixelPosition = None
          centeringWhenEnteredDrag = None
        case _ => ()

    // key press
    scene.onKeyPressed = (event) =>
      event.getCode.toString match
        case "CONTROL" => ()
        case "SHIFT" => ()  // TODO: more keyboard shortcuts
        case _ => ()

    // key release
    scene.onKeyReleased = (event) =>
      event.getCode.toString match
        case "Q" => sim.tool = Tool.FreeBody                                                                  // free body tool
        case "A" => sim.tool = Tool.AutoOrbit                                                                 // auto-orbit tool
        case "Z" => sim.tool = Tool.Nothing                                                                   // "nothing" tool
        case "S" => sim.stopped = !sim.stopped                                                                // start/stop
        case "LEFT" => sim.speed /= 2                                                                         // slow down 0.5x
        case "RIGHT" => sim.speed *= 2                                                                        // speed up 2x
        case "C" => sim.centering = Centering.MassCenter; sim.resetZoom()                                     // center at mass center
        case "V" => sim.centering = Centering.Custom(cursorPosition)                                          // center at cursor position
        case "F" if sim.selectedBody.isDefined => sim.centering = Centering.AtBody(sim.selectedBody.get)      // center at selection
        case "BACK_SPACE" if sim.selectedBody.isDefined => sim.deleteSelection()                              // remove selection
        case "ESCAPE" => sim.deselect()                                                                       // deselect
        case _ => ()


    var lastFrame = 0L

    val timer = AnimationTimer { now =>
        val gc = canvas.graphicsContext2D

        // calculate the current cursor position
        cursorPosition = pixelToPosition(canvas, cursorPixelPosition.x, cursorPixelPosition.y)

        // calculate drag
        cursorDragEnterPixelPosition match
          case Some(enterPixelPosition) =>
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
        sim.zoom += 0.05 * (sim.targetZoom - sim.zoom)
        pixelOffset += 0.05 * (targetPixelOffset - pixelOffset)

        // empty dark space
        gc.fill = Black
        gc.fillRect(0, 0, canvas.width.value, canvas.height.value)

        // draw the paths
        sim.space.bodies.foreach(body =>
          val drawRadius = 5  // TODO: DRY, dynamic radii
          gc.lineWidth = 1
          var pos = positionToPixel(canvas, body.positionHistory.head) + Vector2D(drawRadius, drawRadius)
          for i <- 1 to 255 do
            if body.positionHistory.length > i then
              val posNew: Vector2D = positionToPixel(canvas, body.positionHistory(i)) + Vector2D(drawRadius, drawRadius)
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
          val drawRadius = 5  // TODO: DRY, dynamic radii
          var pos = positionToPixel(canvas, body.position)
          gc.fill = body.color
          gc.fillOval(
            pos.x,
            pos.y,
            drawRadius * 2,
            drawRadius * 2
          )

          // mark a body as selectable (if there are multiple, choose the most massive)
          if (cursorPixelPosition - positionToPixel(canvas, body.position)).norm <= 20 then
            selectableBody match
              case Some(prevBody) =>
                if prevBody.mass < body.mass then selectableBody = Some(body)
              case None => selectableBody = Some(body)

        )


        val drawRadius = 5  // TODO: DRY, dynamic radii

        // outline the selectable body and write its name
        selectableBody.foreach( selection =>
          val pos = positionToPixel(canvas, selection.position)
          gc.stroke = Gray
          gc.fill = selection.color
          gc.strokeRoundRect(pos.x - drawRadius - 10, pos.y - drawRadius - 10, 20 + 4 * drawRadius, 20 + 4 * drawRadius, 10, 10)
          gc.fillText(selection.name, pos.x - drawRadius - 10, pos.y - drawRadius - 15)
      )

        // outline the selected body
        gc.stroke = Red
        sim.selectedBody match
          case Some(body) =>
            val pos = positionToPixel(canvas, body.position)
            gc.strokeRoundRect(pos.x - drawRadius - 10, pos.y - drawRadius - 10, 20 + 4 * drawRadius, 20 + 4 * drawRadius, 10, 10)
          case None => ()

        // update the GUI
        sim.tool match
          case Tool.Nothing => nothingSelector.selected = true
          case Tool.FreeBody => freeBodySelector.selected = true
          case Tool.AutoOrbit => autoOrbitSelector.selected = true

        // draw the FPS & Debug label
        gc.fill = White
        sim.recordFPS(1.0 / deltaTime)
        val avg = sim.getAverageFPS
        gc.fillText(
          s"${(avg).round.toString} FPS | ${(1000 / avg).round.toString} ms | ${sim.speed.roundPlaces(2)} days/s | ${sim.tool} | ${sim.centering} centering | ${sim.zoom.roundPlaces(1)}x zoom" + { if sim.selectedBody.isDefined then s"${sim.selectedBody.get.toString}" else "" },
          20, 20)

    }

    timer.start()


  end start
end GUI