package gui

import scalafx.Includes.jfxColor2sfx
import scalafx.scene.control.{Button, ColorPicker, Label, TextField}
import scalafx.scene.layout.{ColumnConstraints, GridPane, HBox, VBox}
import engine.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.text.Font
import tools.Settings

class BodyListPanel extends GridPane:
  padding = Insets(20, 20, 10, 10)
