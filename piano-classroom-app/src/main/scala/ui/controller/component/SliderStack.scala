package ui.controller.component

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.{InvalidationListener, Observable}
import javafx.event.EventHandler
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.{Font, TextAlignment}

import com.sun.javafx.tk.Toolkit
import ui.controller.component.SliderStack.SliderType
import ui.controller.component.SliderStack.SliderType.SliderType

object SliderStack {
  object SliderType extends Enumeration {
    type SliderType = Value
    val HORIZONTAL, VERTICAL = Value
  }
}

class SliderStack(sliderType: SliderType, color: Color, legend: String) extends Pane {
  val _self = this
  var dragActive = false
  val canvas = new Canvas(getWidth, getHeight)
  val position = new SimpleIntegerProperty()

  position.set(100)

  getChildren.add(canvas)
  setPickOnBounds(false)

  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  position.addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = {
      layoutChildren()
      draw()
    }
  })

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    sliderType match {
      case SliderType.HORIZONTAL =>
        canvas.setLayoutX(0)
        canvas.setLayoutY(position.get() - 10)
        canvas.setWidth(getWidth)
        canvas.setHeight(20)

      case SliderType.VERTICAL =>
        canvas.setLayoutX(position.get() - 10)
        canvas.setLayoutY(0)
        canvas.setWidth(20)
        canvas.setHeight(getHeight)
    }
  }

  canvas.setOnMouseMoved(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      sliderType match {
        case SliderType.HORIZONTAL =>
          _self.getScene.setCursor(Cursor.S_RESIZE)
        case SliderType.VERTICAL =>
          _self.getScene.setCursor(Cursor.E_RESIZE)
      }
    }
  })

  canvas.setOnMouseExited(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      _self.getScene.setCursor(Cursor.DEFAULT)
    }
  })

  canvas.setOnMousePressed(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      dragActive = true
    }
  })

  canvas.setOnMouseReleased(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      dragActive = false
    }
  })

  setOnMouseDragged(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      if(dragActive) {
        sliderType match {
          case SliderType.HORIZONTAL =>
            position.set(Math.min(getHeight, Math.max(0, event.getY - getLayoutY)).toInt)
          case SliderType.VERTICAL =>
            position.set(Math.min(getWidth, Math.max(0, event.getX - getLayoutX)).toInt)
        }
      }
    }
  })

  private def draw(): Unit = {
    val gc = canvas.getGraphicsContext2D
    gc.clearRect(0, 0, canvas.getWidth, canvas.getHeight)

    gc.setStroke(color)
    gc.setLineWidth(1)

    val legendTruncated = legend.take(20)
    val legendWidth = Toolkit.getToolkit.getFontLoader.computeStringWidth(legendTruncated, gc.getFont)

    gc.setFont(new Font(12))
    gc.setTextAlign(TextAlignment.LEFT)
    gc.setTextBaseline(VPos.CENTER)

    sliderType match {
      case SliderType.HORIZONTAL =>
        gc.strokeLine(0, canvas.getHeight / 2, 10, canvas.getHeight / 2)
        gc.strokeLine(20 + legendWidth + 10,canvas.getHeight / 2, canvas.getWidth, canvas.getHeight / 2)
        gc.strokeText(legendTruncated, 20, canvas.getHeight / 2)
      case SliderType.VERTICAL =>
        gc.strokeLine(canvas.getWidth / 2, canvas.getHeight, canvas.getWidth / 2, canvas.getHeight - 20)
        gc.strokeLine(canvas.getWidth / 2, 0, canvas.getWidth / 2, canvas.getHeight - 42)
        gc.strokeText(legendTruncated, canvas.getWidth / 2 - legendWidth / 2, canvas.getHeight - 20 - 12)
    }
  }


}