package tools

import engine.Body
import scalafx.scene.text.Font

/** An object for global (simulation-independent) settings, constants and utility functions
     */
object Settings:
  var theme = "/styles/cupertino-dark.css"

  final val metersPerAU = 149597870700.0
  final val earthRadius = 6371000
  final val earthMass = 5.9722e24
  final val pi = 3.14159

  val fontMain = Font.loadFont(getClass.getResource("/fonts/Inter-Medium.ttf").toExternalForm, 12)
  val fontTitle = Font.loadFont(getClass.getResource("/fonts/Inter-Bold.ttf").toExternalForm, 32)
  val fontMono = Font.loadFont(getClass.getResource("/fonts/JetBrainsMono-Bold.ttf").toExternalForm, 12)

  var showVelocityVectors = true
  var showAccelerationVectors = true
  var showTrails = true
  var showGrid = true
  var showInfo = true

  var multiSelectMode = MultiSelectMode.Heaviest
  var tool = Tool.Nothing

  val random = scala.util.Random()

  /** Format a distance in AU for the scale label
     */
  def formatScale(distanceAU: Double) =
    distanceAU match
      case x if x > 9000000 => f"1e${math.log10(x).round} AU"
      case x if x > 0.9 => f"${x.toInt} AU"
      case x if x > 0.09 => f"$x AU"
      case x => f"${(x * 150000000).toInt} km"

  /** Format a distance in meters for the orbit radius display
     */
  def formatMeters(distance: Double) =
    val au = distance / metersPerAU
    au match
      case x if x > 0.1 => f"$x%.3f AU"
      case x if x > 1 => f"$x%.2f AU"
      case x if x > 10 => f"$x%.1f AU"
      case x if x > 100 => f"$x%.0f AU"
      case x if x < 0.1 => f"${distance / 1000}%.0f km"
      case _ => ""

  /** Generate a help menu for keyboard shortcuts
     */
  def ctrlHelp(sim: Simulation) =
    val bodySpecific = if sim.selectedBody.isDefined then
      val name = sim.selectedBody.get.name
      f"Delete $name  [BACKSPACE]\nFocus at $name          [F]\nCopy $name          [C]\nCut $name          [X]"
    else ""
    f"""
Ctrl + ...

$bodySpecific
Start/Stop      [SPACE]
Free body tool          [Q]
Auto orbit tool          [A]
Pan and zoom tool          [Z]
Speed x0.5  [LEFTARROW]
Speed x2 [RIGHTARROW]
Reset zoom          [R]
Zoom in          [+]
Zoom out          [-]
Center the view          [G]
Center at the cursor          [D]
Paste          [V]

Save          [S]
Save as...    [Shift+S]
New simulation          [N]
Open...          [O]
"""