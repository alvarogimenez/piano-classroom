package ui.controller.component.drawboard

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.{BorderPane, VBox}
import javafx.scene.shape.Rectangle

import ui.controller.monitor.drawboard.{CanvasData, DrawBoardCanvasModel}

class CanvasPreview(model: DrawBoardCanvasModel) extends BorderPane {
  val vbox = new VBox()
  vbox.setAlignment(Pos.CENTER)
  val canvas = new Canvas()
  val label = new Label()

  vbox.getChildren.add(canvas)
  vbox.getChildren.add(label)
  setCenter(vbox)
  setWidth(100)

  model.getCanvasDataProperty.addListener(new ChangeListener[CanvasData] {
    override def changed(observable: ObservableValue[_ <: CanvasData], oldValue: CanvasData, newValue: CanvasData) = {
      updateFromCanvasData(newValue)
    }
  })

  updateFromCanvasData(model.getCanvasData)

  def updateFromCanvasData(canvasData: CanvasData) = {
    label.setText(canvasData.name)
    canvas.setWidth(getWidth)
    canvas.setHeight(getWidth/canvasData.aspectRatio)
    val gc = canvas.getGraphicsContext2D
    model.getDecorator.decorator(
      gc,
      new Rectangle(
        canvas.getLayoutBounds.getMinX,
        canvas.getLayoutBounds.getMinY,
        canvas.getLayoutBounds.getWidth,
        canvas.getLayoutBounds.getHeight
      )
    )
  }
}
