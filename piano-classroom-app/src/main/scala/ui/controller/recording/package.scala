package ui.controller

import javafx.beans.property._
import javafx.collections.{FXCollections, ObservableList}
import javafx.scene.paint.Color

import util.KeyboardNote

import scala.collection.JavaConversions._

package object recording {
  case class RecordingViewport(start: Long, end: Long)

  class RecordingSession(val color: Color) {
    private val start = new SimpleLongProperty()
    private val end = new SimpleObjectProperty[Option[Long]]()
    private val data_ol: ObservableList[RecordingData] = FXCollections.observableArrayList[RecordingData]
    private val data: ListProperty[RecordingData] = new SimpleListProperty[RecordingData](data_ol)
    private val recording_active: BooleanProperty = new SimpleBooleanProperty()

    def getStart: Long = start.get
    def setStart(s: Long): Unit = start.set(s)
    def getStartProperty: LongProperty = start

    def getEnd: Option[Long] = end.get
    def setEnd(e: Option[Long]): Unit = end.set(e)
    def getEndProperty: ObjectProperty[Option[Long]] = end

    def getData: List[RecordingData] = data_ol.toList
    def setData(d: List[RecordingData]) = data.setAll(d)
    def addData(d: RecordingData) = data.add(d)
    def removeData(d: RecordingData) = data.remove(d)
    def getDataProperty: ListProperty[RecordingData] = data

    def isRecordingActive: Boolean = recording_active.get
    def setRecordingActive(recordingActive: Boolean): Unit = recording_active.set(recordingActive)
    def getRecordingActiveProperty: BooleanProperty = recording_active
  }
  
  trait RecordingData {
    val start: Long
    val end: Option[Long]
  }
  case class RecordingNoteData(start: Long, end: Option[Long], note: KeyboardNote) extends RecordingData
  case class RecordingSustainData(start: Long, end: Option[Long]) extends RecordingData
}
