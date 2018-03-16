package ui.controller.component.recording

import javafx.beans.property._
import javafx.beans.{InvalidationListener, Observable}
import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

import ui.renderer.RendererSlave

class RecordingCurrentViewportPreview extends Pane with RendererSlave {
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

  private def draw() = {
    val gc = canvas.getGraphicsContext2D
    gc.setFill(Color.LIGHTGRAY)
    gc.fillRect(0, 0, getWidth, getHeight)
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
