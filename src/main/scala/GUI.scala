import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.control.*
import scalafx.scene.shape.Line

object GUI extends JFXApp3:

  def start() =
    var sim = Simulation()
    var tool: ToolSelected = ToolSelected.AutoOrbit
    var selectedBody: Option[Body] = None

    val minPixelsPerAU = 100.0  // scaling?
    val minMetersPerPixel = 1/minPixelsPerAU * 1.496e11
    var metersPerPixel = minMetersPerPixel
    var pixelOffset: Vector2D = Vector2D(0.0, 0.0)

    def pixelToPosition(canvas: Canvas, pixelX: Double, pixelY: Double): Vector2D =
        (Vector2D(pixelX - canvas.width.value / 2, pixelY - canvas.height.value / 2) + pixelOffset) * metersPerPixel

    def positionToPixel(canvas: Canvas, position: Vector2D): Vector2D =
        (Vector2D(position.x / metersPerPixel + canvas.width.value / 2, position.y / metersPerPixel + canvas.height.value / 2)) - pixelOffset

    val sun = Body(Vector2D(0.0, 0.0))
    sun.radius = 109.076 * 6371000.0
    sun.mass = 333030.262 * 5.9722e24
    sun.color = Yellow

    val sun1 = Body(Vector2D(0.0, 3 * 149597870700.0))
    sun1.radius = 109.076 * 6371000.0
    sun1.mass = 333030.262 * 5.9722e24

    val earth = Body(Vector2D(149597870700.0, 0.0))
    earth.radius = 6371000.0
    earth.mass = 5.9722e24
    earth.velocity = Vector2D(0.0, -29780.0)
    earth.color = Blue

    val moon = Body(Vector2D(149984400000.0, 0.0))
    moon.radius = 0.2727 * 6371000.0
    moon.mass = 0.0123000371 * 5.9722e24
    moon.velocity = Vector2D(0.0, -28758.0)

    sim.space.addBody(sun)
    sim.space.addBody(earth)
    sim.space.addBody(moon)
    selectedBody = Some(earth)
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

    val sidePanel = VBox()
    val spaceView = new Pane()
    val canvas = new Canvas():

      // mouse click
      onMouseClicked = (event) =>
        val clickPosition = pixelToPosition(this, event.getX, event.getY)

        // create a free body
        if tool == ToolSelected.FreeBody then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addBody(body)

        // create an auto orbit
        else if tool == ToolSelected.AutoOrbit && selectedBody.isDefined then
          val body = Body(clickPosition)
          body.mass = 5.9722e24
          body.radius = 6371000.0
          sim.space.addAutoOrbit(body, selectedBody.get)


    canvas.width <== spaceView.width
    canvas.height <== spaceView.height

    spaceView.children.add(canvas)

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

    // Set background colors for the boxes
    sidePanel.background = Background.fill(Gray)

    val scene = Scene(parent = root)

    stage.scene = scene

    // key press
    scene.onKeyReleased = (event) =>
      event.getText match
        case "q" => tool = ToolSelected.FreeBody
        case "a" => tool = ToolSelected.AutoOrbit
        case "z" => tool = ToolSelected.Nothing
        case _ => ()


    var lastFrame = 0L

    val timer = AnimationTimer { now =>
        val gc = canvas.graphicsContext2D

        metersPerPixel = math.max(minMetersPerPixel, 0.0)
        pixelOffset = sim.space.massCenter / metersPerPixel
        val deltaTime = (now - lastFrame) / 1e9

        if lastFrame > 0 then sim.space.tick(deltaTime * 86400 * sim.speed * 25)
        lastFrame = now

        // empty dark space
        gc.fill = Black
        gc.fillRect(0, 0, canvas.width.value, canvas.height.value)

        // draw the paths
        sim.space.bodies.foreach(body =>
          val drawRadius = 5
          gc.lineWidth = 2
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
        sim.space.bodies.foreach(body =>
          val drawRadius = 5
          var pos = positionToPixel(canvas, body.position)
          gc.fill = body.color
          gc.fillOval(
            pos.x,
            pos.y,
            drawRadius * 2,
            drawRadius * 2
          )
        )

        // draw the FPS & Debug label
        gc.fill = White
        sim.recordFPS(1.0 / deltaTime)
        val avg = sim.getAverageFPS
        gc.fillText(
          s"${(avg).round.toString} FPS | ${(1000 / avg).round.toString} ms | ${sim.speed} days/s | $tool" + { if selectedBody.isDefined then s"${selectedBody.get.toString}" else "" },
          20, 20)

    }

    timer.start()


  end start
end GUI