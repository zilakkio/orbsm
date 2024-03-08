package tools
import java.nio.file.{Files, Paths}
import upickle.default.*
import engine.*
import scalafx.scene.paint.Color.*
import scalafx.scene.paint.Color

import java.nio.charset.StandardCharsets


implicit val vectorRW: ReadWriter[Vector3D] = readwriter[ujson.Value].bimap[Vector3D](
  vector3D => ujson.Arr(vector3D.x, vector3D.y, vector3D.z),
  json => {
    val arr = json.arr
    if arr.length == 2 then
      Vector3D(arr(0).num, arr(1).num)
    else if arr.length == 3 then
      Vector3D(arr(0).num, arr(1).num, arr(2).num)
    else
      Vector3D(0, 0, 0)
  }
)

implicit val colorRW: ReadWriter[Color] = readwriter[ujson.Value].bimap[Color](
  color => {
    val red = (color.red * 255).round.toInt
    val green = (color.green * 255).round.toInt
    val blue = (color.blue * 255).round.toInt
    val hex = f"$red%02x$green%02x$blue%02x"
    ujson.Str(hex)
  },
  json => {
    val hex = json.str
    val red = Integer.parseInt(hex.substring(0, 2), 16)
    val green = Integer.parseInt(hex.substring(2, 4), 16)
    val blue = Integer.parseInt(hex.substring(4, 6), 16)
    Color.rgb(red, green, blue)
  }
)

implicit val bodyRW: ReadWriter[Body] = readwriter[ujson.Value].bimap[Body](
  body => ujson.Obj(
    "name" -> body.name,
    "mass" -> body.massEarths,
    "radius" -> body.radiusEarths,
    "position" -> writeJs(body.position / 149597870700.0)(vectorRW),
    "velocity" -> writeJs(body.velocity)(vectorRW),
    "color" -> writeJs(body.color)(colorRW)
  ),
  json => {
    val obj = json.obj
    val body = Body(
      read[Vector3D](obj("position"))(vectorRW) * 149597870700.0,
      obj("name").str,
    )
    val massEarths = obj("mass").num
    val radiusEarths = obj("radius").num
    body.velocity = read[Vector3D](obj("velocity"))(vectorRW)
    body.mass = massEarths * 5.9722e24
    body.radius = radiusEarths * 6371000
    body.color = read[Color](obj("color"))(colorRW)
    body
  }
)

implicit val spaceRW: ReadWriter[SimulationSpace] = readwriter[ujson.Value].bimap[SimulationSpace](
  space => ujson.Obj(
    "bodies" -> ujson.Arr(space.bodies.map(body => writeJs(body)(bodyRW)).toArray: _*),
  ),
  json => {
    val space = SimulationSpace()
    val arr = json.obj("bodies").arr
    for body <- arr do
      space.addBody(read[Body](body)(bodyRW))
    space.interactionForces = space.interactionForces.appended(GravitationalForce)
    space
  }
)
  
implicit val simulationRW: ReadWriter[Simulation] = readwriter[ujson.Value].bimap[Simulation](
  sim => ujson.Obj(
    "fps" -> sim.fps,
    "tpf" -> sim.tpf,
    "speed" -> sim.speed,
    "stopped" -> sim.stopped,
    "space" -> writeJs(sim.space)(spaceRW)
  ),
  json => {
    val obj = json.obj
    val sim = Simulation()

    sim.fps = obj("fps").num.toInt
    sim.tpf = obj("tpf").num.toInt
    sim.speed = obj("speed").num.toInt
    sim.stopped = obj("stopped").bool
    sim.space = read[SimulationSpace](obj("space"))(spaceRW)

    sim
  }
)


/**
 * A utility object for handling file I/O
 */
object FileHandler:

  /**
   * Create/update a simulation file
   *
   * @param fileName   The target file relative path.
   * @param simulation The simulation to be saved.
   * @return
   */
  def save(fileName: String, simulation: Simulation) =
    val json = write(simulation)
    Files.write(Paths.get(fileName), json.getBytes(StandardCharsets.UTF_8))


  /**
   * Restore a simulation from file
   *
   * @param fileName The source file relative path.
   * @return The restored simulation.
   */
  def load(fileName: String): Simulation =
    val json = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8)
    read[Simulation](json)
