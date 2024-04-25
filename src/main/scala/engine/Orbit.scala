package engine

import engine.{Body, Vector3D}
import scalafx.scene.canvas.GraphicsContext
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Blue, Gray, Green, LightGreen, Red}
import tools.{Settings, Simulation}

case class Orbit(val sim: Simulation, val baseDistance: Double, val launchPosition: Vector3D, val parent: Body):

  private val focus: Vector3D = parent.position
  private val apsis: Vector3D = launchPosition
  private val launchDistance: Double = (apsis - focus).norm
  private val gc: GraphicsContext = sim.canvas.graphicsContext2D

  val eccentricity: Double =
    (launchDistance - baseDistance) / (launchDistance + baseDistance)

  var semiMajor: Double = ( (focus - apsis).norm / (1 + eccentricity) )
  val semiMinor: Double = semiMajor * math.sqrt(1 - eccentricity * eccentricity)

  private val fociDistance = 2 * semiMajor * eccentricity

  val center = focus - fociDistance / 2 * (focus - apsis).unit
  val angle = math.toDegrees(math.atan2(focus.y - center.y, focus.x - center.x))
  val pericenter = math.min(launchDistance, baseDistance)
  val apocenter = math.max(launchDistance, baseDistance)
  
  /** Convert a position vector to pixel position
     */
  private def px(pos: Vector3D): Vector3D =
    sim.positionToPixel(pos)
  
  /** Convert a length to pixel length
     */
  private def px(a: Double): Double =
    a / sim.metersPerPixel
  
  /** Draw the orbit
     */
  def draw() =
    gc.save()
    gc.translate(px(center).x, px(center).y)
    gc.rotate(angle)
    gc.stroke = Color.Gray
    gc.strokeOval(-px(semiMajor), -px(semiMinor), px(semiMajor) * 2, px(semiMinor) * 2)
    if eccentricity.abs <= 1e-6 then
      gc.fill = Gray
      gc.font = Settings.fontMono
      gc.fillText(f" ${Settings.formatMeters(semiMajor)}", -px(semiMajor), 0)
    gc.restore()
    
  /** The initial velocity
     */
  def velocity: Vector3D =
    val mu = GravitationalForce.G * parent.mass
    val r = (apsis - focus).norm
    val v = math.sqrt(mu * (2 / r - 1 / semiMajor))
    val direction = (apsis - focus).unit.cross(Vector3D(0, 0, 1))
    direction * v

  override def toString: String = f"""Orbit parameters
a = ${Settings.formatMeters(semiMajor)}
b = ${Settings.formatMeters(semiMinor)}
e = ${eccentricity}%.2f
angle = ${angle}%.2f°
pericenter distance = ${Settings.formatMeters(pericenter)}
apocenter distance = ${Settings.formatMeters(apocenter)}
"""