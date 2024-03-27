package tools

import scalafx.scene.text.Font


object Settings:
  var theme = "/styles/cupertino-dark.css"

  final val metersPerAU = 149597870700.0
  final val earthRadius = 6371000
  final val earthMass = 5.9722e24
  final val pi = 3.14

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