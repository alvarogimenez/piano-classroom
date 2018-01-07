import javafx.scene.paint.Color

package object util {
  def colorToWebHex(c: Color) = {
    "#%02X%02X%02X".format((c.getRed * 255).toInt, (c.getGreen * 255).toInt, (c.getBlue * 255).toInt)
  }
}
