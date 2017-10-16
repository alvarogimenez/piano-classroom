package ui.controller.monitor

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class MonitorDrawBoardModel {
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator
}

class MonitorDrawBoardController(model: MonitorDrawBoardModel) {

  def initialize() = {

  }

  def start() = {
    model.setDecorator(GraphicsDecorator({ case (gc: GraphicsContext, r: Rectangle) =>
      gc.setFill(Color.WHITE)
      gc.fillRect(r.getX, r.getY, r.getWidth, r.getHeight)
      gc.setStroke(Color.BLUE)
      gc.strokeLine(r.getX, r.getY, r.getX + r.getWidth, r.getY + r.getHeight)
      println(System.currentTimeMillis())
    }))
  }

  def stop() = {

  }
}
