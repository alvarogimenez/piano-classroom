package ui.controller.component.drawboard

import javafx.event.{ActionEvent, EventHandler}
import javafx.geometry.VPos
import javafx.scene.control._
import javafx.scene.input.ContextMenuEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.{Font, Text, TextAlignment}


class PaletteColorButton(val color: Color, val millis: Int) extends Button {
  val textColor = if(color.grayscale().getRed < 0.5) Color.WHITE else Color.BLACK

  val p = new Pane()
  p.setMinWidth(40)
  p.setMaxWidth(40)
  p.setMinHeight(40)
  p.setMaxHeight(40)

  val c = new Circle()
  c.setFill(color)
  c.setRadius(12)
  c.setCenterX(20)
  c.setCenterY(20)

  val t = new Text(s"${millis}m")
  t.setStroke(textColor)
  t.setWrappingWidth(40)
  t.setTextOrigin(VPos.CENTER)
  t.setTextAlignment(TextAlignment.CENTER)
  t.setFont(new Font(10))
  t.setX(0)
  t.setY(20)

  p.getChildren.addAll(c, t)

  setMinWidth(40)
  setMaxWidth(40)
  setMinHeight(40)
  setMaxHeight(40)
  setGraphic(p)

  val _self = this
  var deleteHandler: EventHandler[ActionEvent] = _

  def setOnDelete(handler: EventHandler[ActionEvent]): Unit = deleteHandler = handler

  this.setOnContextMenuRequested(new EventHandler[ContextMenuEvent] {
    override def handle(event: ContextMenuEvent) = {
      val contextMenu = new ContextMenu()

      val contextMenu_delete = new MenuItem("Delete")
      contextMenu_delete.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          if(deleteHandler != null) {
            deleteHandler.handle(event)
          }
        }
      })

      contextMenu.getItems.add(contextMenu_delete)

      contextMenu.show(_self, event.getScreenX, event.getScreenY)
    }
  })
}
