package ui.controller.component

import javafx.scene.control.Button
import javafx.scene.paint.Color

import scala.util.Random


class ProfileButton(name: String) extends Button {
  setText(name)
  getStyleClass.add("profile-button")

  val backgroundColor = new Color(Random.nextDouble(), Random.nextDouble(), Random.nextDouble(), 1).desaturate()
  val textColor = if(backgroundColor.grayscale().getRed < 0.5) Color.WHITE else Color.BLACK

  setStyle(s"-fx-background-color: ${getHexColor(backgroundColor)}; -fx-text-fill: ${getHexColor(textColor)}")

  def getHexColor(c: Color) = {
    "#%02X%02X%02X".format((c.getRed * 255).toInt, (c.getGreen * 255).toInt, (c.getBlue * 255).toInt)
  }
}
