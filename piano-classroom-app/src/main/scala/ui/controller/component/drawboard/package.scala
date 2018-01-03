package ui.controller.component

import javafx.scene.paint.Color
import javafx.scene.shape.Path


package object drawboard {
  case class Pen(
    size: Double,
    color: Color
  )

  trait ActionStatus
  case class ActionFreeDraw(id: String, path: Path, pen: Pen) extends ActionStatus
  case class ActionFreeErase() extends ActionStatus

  object DrawBoardAction extends Enumeration {
    type DrawBoardAction = Value
    val FREE_DRAW, ERASE = Value
  }
}
