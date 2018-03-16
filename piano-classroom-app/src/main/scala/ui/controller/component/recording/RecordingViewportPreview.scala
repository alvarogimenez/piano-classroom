package ui.controller.component.recording

import javafx.beans.property._
import javafx.beans.{InvalidationListener, Observable}
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

import context.Context
import ui.controller.recording.{RecordingModel, RecordingViewport}
import ui.renderer.RendererSlave

class RecordingViewportPreview(model: RecordingModel) extends Pane with RendererSlave {
  private var dragDeltaX: Long = _
  private var dragActive = false

  private val playback_position: LongProperty = new SimpleLongProperty()

  def getPlaybackPosition: Long = playback_position.get
  def setPlaybackPosition(playbackPosition: Long): Unit = playback_position.set(playbackPosition)
  def getPlaybackPositionProperty: LongProperty = playback_position

  private val canvas = new Canvas(getWidth, getHeight)
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

  canvas.setOnMousePressed(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      val (viewportX1, viewportX2) = getViewportXs
      if(event.getX.toInt >= viewportX1 && event.getX.toInt <= viewportX2) {
        dragActive = true
        dragDeltaX = (event.getX - viewportX1).toLong
      }
    }
  })

  canvas.setOnMouseReleased(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      if(dragActive) {
        dragActive = false
      } else {
        val (viewportX1, viewportX2) = getViewportXs
        viewportPositionFromMouseX(event.getX.toInt, ((viewportX2 - viewportX1)/2).toLong)
      }
    }
  })

  canvas.setOnMouseDragged(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      if(dragActive) {
        viewportPositionFromMouseX(event.getX.toInt, dragDeltaX)
      }
    }
  })

  private def getViewportXs = {
    val previewMax = calculateMaxPreviewX
    val viewportX1 = model.getRecordingViewport.start * getWidth / previewMax
    val viewportX2 = model.getRecordingViewport.end * getWidth / previewMax
    (viewportX1, viewportX2)
  }

  private def viewportPositionFromMouseX(x: Int, deltaX: Long) = {
    val previewMax = calculateMaxPreviewX
    val (viewportX1, viewportX2) = getViewportXs
    val minX = deltaX
    val maxX = getWidth - (viewportX2 - viewportX1) + deltaX
    val validPosition = Math.max(Math.min(maxX, x), minX)

    val newRecordingViewportStart = ((validPosition - deltaX)*previewMax/getWidth).toLong
    val recordingViewportWidth = model.getRecordingViewport.end - model.getRecordingViewport.start
    model.setRecordingViewport(RecordingViewport(newRecordingViewportStart, newRecordingViewportStart + recordingViewportWidth))
  }

  private def calculateMaxPreviewX = {
    val playbackTime = Context.playbackService.getPlaybackTime
    (
      List(30000, playbackTime, model.getRecordingViewport.end) ++
        model.getRecordingTracks.flatMap(_.getRecordingSessions).map(_.getEnd.getOrElse(playbackTime))
      ).max
  }

  private def draw() = {
    val gc = canvas.getGraphicsContext2D
    gc.setFill(Color.LIGHTGRAY)
    gc.fillRect(0, 0, getWidth, getHeight)

    val playbackTime = Context.playbackService.getPlaybackTime
    val previewMax = calculateMaxPreviewX
    val trackHeight = getHeight / model.getRecordingTracks.size
    model.getRecordingTracks.zipWithIndex.foreach { case (recordingTrack, index) =>
      val y = index * trackHeight
      recordingTrack.getRecordingSessions.foreach { recordingSession =>
        val x1 = recordingSession.getStart * getWidth / previewMax
        val x2 = recordingSession.getEnd.getOrElse(playbackTime) * getWidth / previewMax
        gc.setFill(recordingSession.color)
        gc.fillRect(x1, y, x2 - x1, trackHeight)
      }
    }

    val playbackX = playbackTime * getWidth / previewMax
    gc.setLineWidth(1)
    gc.strokeLine(playbackX, 0, playbackX, getHeight)

    val (viewportX1, viewportX2) = getViewportXs
    gc.setStroke(Color.BLACK)
    gc.setLineWidth(2)
    gc.strokeRect(viewportX1 + 1, 1, viewportX2 - viewportX1 - 2, getHeight - 2)
  }

  private val _visibleProperty = new SimpleBooleanProperty()
  _visibleProperty.bind(this.impl_treeVisibleProperty())

  def isNodeVisible:Boolean = _visibleProperty.get()

  def render(): Unit = {
    if(isNodeVisible) {
      draw()
    }
  }
}
