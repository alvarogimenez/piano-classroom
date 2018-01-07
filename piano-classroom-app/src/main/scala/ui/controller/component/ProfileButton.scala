package ui.controller.component

import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.control._
import javafx.scene.input.ContextMenuEvent
import javafx.scene.paint.Color

import util._

class ProfileButton(name: String, color: Color) extends Button {
  setText(name)
  getStyleClass.add("profile-button")

  private val _self: Button = this
  private val textColor: Color = if(color.grayscale().getRed < 0.5) Color.WHITE else Color.BLACK
  private var onDelete: EventHandler[ActionEvent] = _

  setStyle(s"-fx-background-color: ${colorToWebHex(color)}; -fx-text-fill: ${colorToWebHex(textColor)}")

  setOnContextMenuRequested(new EventHandler[ContextMenuEvent] {
    override def handle(event: ContextMenuEvent) = {
      val contextMenu = new ContextMenu()
      val contextMenu_delete = new MenuItem("Delete")
      contextMenu_delete.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          if(onDelete != null) {
            onDelete.handle(event)
          }
        }
      })
      contextMenu.getItems.add(contextMenu_delete)
      contextMenu.show(_self, event.getScreenX, event.getScreenY)
    }
  })

  def setOnDelete(eventHandler: EventHandler[ActionEvent]): Unit = {
    onDelete = eventHandler
  }
}
