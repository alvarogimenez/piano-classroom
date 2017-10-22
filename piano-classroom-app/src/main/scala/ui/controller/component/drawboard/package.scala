package ui.controller.component

import javafx.scene.shape.Path


package object drawboard {
  trait ActionStatus
  case class ActionFreeDraw(id: String, path: Path) extends ActionStatus
  case class ActionFreeErase() extends ActionStatus

  object DrawBoardAction extends Enumeration {
    type DrawBoardAction = Value
    val FREE_DRAW = Value
  }
}
