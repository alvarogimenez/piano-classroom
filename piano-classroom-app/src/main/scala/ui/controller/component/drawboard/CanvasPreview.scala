package ui.controller.component.drawboard

import java.lang.Boolean
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.{BorderPane, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font

import ui.controller.monitor.drawboard.{CanvasData, DrawBoardCanvasModel}

class CanvasPreview(model: DrawBoardCanvasModel) extends BorderPane {
  val vbox = new VBox()
  vbox.setAlignment(Pos.CENTER)
  val canvas = new Canvas()
  val label = new Label()
  label.setFont(Font.font(label.getFont.getFamily, 10))

  vbox.getChildren.add(canvas)
  vbox.getChildren.add(label)
  setCenter(vbox)
  setWidth(100)

  model.getCanvasDataProperty.addListener(new ChangeListener[CanvasData] {
    override def changed(observable: ObservableValue[_ <: CanvasData], oldValue: CanvasData, newValue: CanvasData) = {
      updateFromCanvasData(newValue)
    }
  })

  model.getSelectedProperty.addListener(new ChangeListener[Boolean] {
    override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = {
      updateFromCanvasData(model.getCanvasData)
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
    if(model.isSelected) {
      gc.setStroke(Color.BLUE)
      gc.setLineWidth(3)
      gc.strokeRect(0, 0, canvas.getWidth, canvas.getHeight)
    }
  }
}
