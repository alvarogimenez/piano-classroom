package ui.controller.monitor.drawboard

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.{LineTo, MoveTo, Rectangle}

import ui.controller.monitor.GraphicsDecorator

import scala.collection.JavaConversions._

class DrawBoardCanvasModel {
  val canvas_data: SimpleObjectProperty[CanvasData] = new SimpleObjectProperty[CanvasData]()
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()

  def getCanvasData: CanvasData = canvas_data.get()
  def setCanvasData(c: CanvasData): Unit = canvas_data.set(c)
  def getCanvasDataProperty: SimpleObjectProperty[CanvasData] = canvas_data

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator

  canvas_data.addListener(new ChangeListener[CanvasData] {
    override def changed(observable: ObservableValue[_ <: CanvasData], oldValue: CanvasData, newValue: CanvasData) = {
      setDecorator(
        GraphicsDecorator({ case (gc: GraphicsContext, r: Rectangle) =>
            gc.setFill(Color.WHITE)
            gc.fillRect(r.getX, r.getY, r.getWidth, r.getHeight)
            newValue
              .shapes
              .foreach {
                case CanvasLine(id, path) =>
                  gc.setStroke(Color.BLACK)
                  gc.setLineWidth(r.getWidth*3.0/1000.0)
                  gc.beginPath()
                  path.getElements.toList.foreach {
                    case x: MoveTo => gc.moveTo(r.getX + r.getWidth * x.getX, r.getY + r.getHeight * x.getY)
                    case x: LineTo => gc.lineTo(r.getX + r.getWidth * x.getX, r.getY + r.getHeight * x.getY)
                    case _ =>
                  }
                  gc.stroke()
                case _ =>
              }
        })
      )
    }
  })
}
