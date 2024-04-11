package gui

import scalafx.scene.image.{Image, ImageView}


object Icons:
  val nameMap = Map(
    "box-3" -> "box-3-line.png",
    "circle" -> "circle-line.png",
    "clipboard" -> "clipboard-line.png",
    "close-circle" -> "close-circle-line.png",
    "delete-bin" -> "delete-bin-line.png",
    "drag-move" -> "drag-move-2-fill.png",
    "file-copy" -> "file-copy-line.png",
    "focus-3" -> "focus-3-line.png",
    "focus" -> "focus-line.png",
    "focus-mode" -> "focus-mode.png",
    "formula" -> "formula.png",
    "grid" -> "grid-line.png",
    "infinity" -> "infinity-line.png",
    "meteor" -> "meteor-line.png",
    "moon" -> "moon-line.png",
    "pause" -> "pause-fill.png",
    "planet" -> "planet-line.png",
    "play" -> "play-fill.png",
    "play-reverse" -> "play-reverse-fill.png",
    "restart" -> "restart-line.png",
    "rewind" -> "rewind-fill.png",
    "rocket-2" -> "rocket-2-line.png",
    "route" -> "route-line.png",
    "save-3" -> "save-3-line.png",
    "shining" -> "shining-line.png",
    "speed" -> "speed-fill.png",
    "speed-up" -> "speed-up-fill.png",
    "star" -> "star-line.png",
    "sun" -> "sun-line.png",
    "temp-cold" -> "temp-cold-line.png",
    "typhoon" -> "typhoon-line.png",
    "weight" -> "weight-line.png",
    "info" -> "information-2-line.png",
    "color" -> "palette-line.png",
    "time" -> "time-line.png",
    "orbit" -> "orbit.png",
    "a" -> "a.png",
    "v" -> "v.png",
    "distance" -> "distance.png",
    "radius" -> "radius.png",
  )

  def get(name: String): ImageView = new ImageView(Image(getClass.getResourceAsStream(s"/icons/${nameMap(name)}"))) {fitWidth = 24; fitHeight = 24}