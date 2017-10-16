package ui.controller

import javafx.scene.canvas.GraphicsContext
import javafx.scene.shape.Rectangle


package object monitor {
  case class WebCamSource(
    name: String,
    index: Int
  ) {
    override def toString(): String = name
  }

  case class GraphicsDecorator(
    decorator: (GraphicsContext, Rectangle) => Unit
  )

  object MonitorSource extends Enumeration {
    type MonitorSource = Value
    val CAMERA, PENCIL, BOARD, MUSIC = Value
  }
}
