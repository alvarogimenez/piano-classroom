package ui.controller.component.drawboard

import java.util.UUID
import javafx.beans.{InvalidationListener, Observable}
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.layout.BorderPane
import javafx.scene.shape.{LineTo, MoveTo, Path, Rectangle}

import ui.controller.component.drawboard.DrawBoardAction.DrawBoardAction
import ui.controller.monitor.drawboard.{CanvasLine, DrawBoardCanvasModel}

import scala.collection.JavaConversions._

class DrawBoardCanvas(model: DrawBoardCanvasModel) extends BorderPane {
  private var _action: Option[DrawBoardAction] = None
  private var _status: Option[ActionStatus] = None

  val canvas = new Canvas()
  _action = Some(DrawBoardAction.FREE_DRAW)

  getChildren.add(canvas)

  var updateHandler: EventHandler[MouseEvent] = _
  def setUpdateHandler(handler: EventHandler[MouseEvent]): Unit = updateHandler = handler

  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })

  canvas.setOnMousePressed(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      _action match {
        case Some(DrawBoardAction.FREE_DRAW) =>
          if(event.getButton == MouseButton.PRIMARY) {
            val path = new Path()
            path.getElements.add(new MoveTo(event.getX / canvas.getWidth, event.getY / canvas.getHeight))
            _status = Some(ActionFreeDraw(UUID.randomUUID().toString, path, model.getPen))
          } else {
            _status = Some(ActionFreeErase())
          }
        case _ =>
      }
    }
  })
  canvas.setOnMouseDragged(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      _status = _status match {
        case Some(ActionFreeDraw(id, path, pen)) =>
          path.getElements.add(new LineTo(event.getX / canvas.getWidth, event.getY / canvas.getHeight))
          model.setCanvasData(model.getCanvasData.copy(shapes = model.getCanvasData.shapes.filterNot(_.id == id) ++ Set(CanvasLine(id, path, pen.size, pen.color))))
          _status
        case Some(ActionFreeErase()) =>
          model.setCanvasData(model.getCanvasData.copy(shapes = model.getCanvasData.shapes.filterNot {
            case CanvasLine(id, path, _, _) =>
              val denormalizedPath = new Path()
              path.getElements.toList.foreach {
                case x: MoveTo => denormalizedPath.getElements.add(new MoveTo(x.getX * canvas.getWidth, x.getY * canvas.getHeight))
                case x: LineTo => denormalizedPath.getElements.add(new LineTo(x.getX * canvas.getWidth, x.getY * canvas.getHeight))
              }
              denormalizedPath.intersects(event.getX , event.getY, 5.0, 5.0)
            case _=> false
          }))
          _status
        case _ =>
          _status
      }
    }
  })
  canvas.setOnMouseReleased(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      if(updateHandler != null) {
        updateHandler.handle(event)
      }
    }
  })

  override def layoutChildren(): Unit = {
    super.layoutChildren()

    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())

    val width = snapSize(getWidth) - snappedLeftInset() - snappedRightInset()
    val height = width / model.getCanvasData.aspectRatio

    canvas.setWidth(width)
    canvas.setHeight(height)

    draw()
  }

  model.getDecoratorProperty.addListener(new InvalidationListener {
    override def invalidated(observable: Observable) = {
      draw()
    }
  })

  draw()

  def draw() = {
    val gc = canvas.getGraphicsContext2D
    model.getDecorator.decorator(
      gc,
      new Rectangle(
        0,
        0,
        canvas.getLayoutBounds.getWidth,
        canvas.getLayoutBounds.getHeight
      )
    )
  }
}
