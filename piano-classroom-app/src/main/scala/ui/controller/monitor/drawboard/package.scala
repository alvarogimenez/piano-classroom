package ui.controller.monitor

import javafx.scene.paint.Color
import javafx.scene.shape.{LineTo, MoveTo, Path, Rectangle}


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

  object CanvasLine {
    val LineToRegex = """L:(-?[0-9]+\.[0-9]+),(-?[0-9]+\.[0-9]+)""".r
    val MoveToRegex = """M:(-?[0-9]+\.[0-9]+),(-?[0-9]+\.[0-9]+)""".r

    def pathFromString(s: String): Path = {
      val path = new Path()
      val items: List[String] = s.split(";").toList
      items.foreach {
        case LineToRegex(n1, n2) => path.getElements.add(new LineTo(n1.toDouble, n2.toDouble))
        case MoveToRegex(n1, n2) => path.getElements.add(new MoveTo(n1.toDouble, n2.toDouble))
        case _ =>
      }
      path
    }

    def pathToString(p: Path): String = {
      import scala.collection.JavaConversions._

      p.getElements.toList.map {
        case x: MoveTo => s"M:${x.getX},${x.getY}"
        case x: LineTo => s"L:${x.getX},${x.getY}"
        case _ =>
      }.mkString(";")
    }
  }

  case class CanvasLine(id: String, path: Path, size: Double, color: Color) extends CanvasShape {
    override def equals(obj: scala.Any): Boolean = false
  }
}
