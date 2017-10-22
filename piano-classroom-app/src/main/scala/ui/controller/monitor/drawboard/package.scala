package ui.controller.monitor

import javafx.scene.shape.{Path, Rectangle}


package object drawboard {
  case class CanvasData(
    name: String,
    aspectRatio: Double,
    fullscreenViewport: Rectangle,
    shapes: Set[CanvasShape]
  )

  trait CanvasShape {
    val id: String
  }
  case class CanvasLine(id: String, path: Path) extends CanvasShape {
    override def equals(obj: scala.Any): Boolean = false
  }
}
