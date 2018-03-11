package ui.controller.component.mixer

import javafx.beans.{InvalidationListener, Observable}
import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

class CompressorPreview extends Pane {
  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())
    canvas.setWidth(snapSize(getWidth) - snappedLeftInset() - snappedRightInset())
    canvas.setHeight(snapSize(getHeight) - snappedTopInset() - snappedBottomInset())
  }
  private def draw(): Unit = {
    val gc = canvas.getGraphicsContext2D
    gc.clearRect(0, 0, getWidth, getHeight)

    gc.setFill(Color.WHITE)
    gc.fillRect(0, 0, getWidth, getHeight)

    gc.setStroke(Color.gray(0.7))
    gc.setLineWidth(1)
    gc.strokeRect(0, 0, getWidth, getHeight)

    gc.setStroke(Color.BLACK)
    gc.setLineWidth(1)
    gc.strokeLine(0, getHeight, getWidth, 0)
  }


}