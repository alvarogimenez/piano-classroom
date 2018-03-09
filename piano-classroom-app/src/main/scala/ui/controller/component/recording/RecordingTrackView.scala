package ui.controller.component.recording

import javafx.beans.property._
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.canvas.Canvas
import javafx.scene.layout.{BorderPane, Pane}
import javafx.scene.paint.Color

import ui.controller.recording.{RecordingNoteData, RecordingSession, RecordingViewport}
import ui.renderer.RendererSlave

import scala.collection.JavaConversions._

class RecordingTrackView extends Pane with RendererSlave {
  private val recording_viewport: ObjectProperty[RecordingViewport] = new SimpleObjectProperty[RecordingViewport]()
  private val playback_position: LongProperty = new SimpleLongProperty()
  private val recording_sessions_ol: ObservableList[RecordingSession] = FXCollections.observableArrayList[RecordingSession]
  private val recording_sessions: ListProperty[RecordingSession] = new SimpleListProperty[RecordingSession](recording_sessions_ol)

  def getRecordingSessions: List[RecordingSession] = recording_sessions.toList
  def getRecordingSessionsProperty: ListProperty[RecordingSession] = recording_sessions

  def getPlaybackPosition: Long = playback_position.get
  def setPlaybackPosition(playbackPosition: Long): Unit = playback_position.set(playbackPosition)
  def getPlaybackPositionProperty: LongProperty = playback_position

  def getRecordingViewport: RecordingViewport = recording_viewport.get
  def setRecordingViewport(recordingViewport: RecordingViewport): Unit = recording_viewport.set(recordingViewport)
  def getRecordingViewportProperty: ObjectProperty[RecordingViewport] = recording_viewport

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
    gc.setFill(Color.WHITE)
    gc.fillRect(0, 0, getWidth, getHeight)

    val noteIndexes = getRecordingSessions.flatMap(_.getData).collect { case e: RecordingNoteData => e}.map(_.note.absoluteIndex())

    println(s"-------------")
    getRecordingSessions.sortBy { r => if(r.isRecordingActive) 1 else 0 }.foreach { recordingSession =>
      println(s"start = ${recordingSession.getStart}, end = ${recordingSession.getEnd}")
      val  x1 = Math.max(0, (recordingSession.getStart - getRecordingViewport.start) * getWidth / (getRecordingViewport.end - getRecordingViewport.start))
      val  x2 = Math.min(getWidth, (recordingSession.getEnd.getOrElse(getPlaybackPosition) - getRecordingViewport.start) * getWidth / (getRecordingViewport.end - getRecordingViewport.start))
      gc.setFill(recordingSession.color)
      gc.fillRect(x1, 0, x2, getHeight)

      if(noteIndexes.nonEmpty) {
        val minNote = noteIndexes.min
        val maxNote = noteIndexes.max

        gc.setFill(Color.BLACK)
        recordingSession.getData.foreach {
          case d: RecordingNoteData =>
            val y = (d.note.absoluteIndex() - minNote) / (maxNote - minNote).toDouble * getHeight
            val h = getHeight / (maxNote - minNote)
            val x1 = (d.start - getRecordingViewport.start) * getWidth / (getRecordingViewport.end - getRecordingViewport.start)
            val x2 = (d.end.getOrElse(getRecordingViewport.end) - getRecordingViewport.start) * getWidth / (getRecordingViewport.end - getRecordingViewport.start)

            if (x1 <= getWidth && x2 >= 0) {
              gc.fillRect(x1, getHeight - y, x2 - x1, h)
            }
          case _ =>
        }
      }
    }
    println(s"-------------")

    val playbackRelative = getPlaybackPosition - getRecordingViewport.start
    if(playbackRelative >= 0 && playbackRelative < getRecordingViewport.end) {
      val playbackX = playbackRelative * getWidth / (getRecordingViewport.end - getRecordingViewport.start)
      gc.setStroke(Color.BLACK)
      gc.strokeLine(playbackX, 0, playbackX, getHeight)
    }
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
