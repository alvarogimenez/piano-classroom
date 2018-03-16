package ui.controller.recording

import java.lang.Boolean
import javafx.beans.property._
import javafx.collections.{FXCollections, ObservableList}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control._
import javafx.scene.layout.BorderPane

import context.Context
import services.audio.channel.MidiChannel
import ui.controller.component.recording.RecordingTrackView
import ui.controller.global.ProjectSessionUpdating

import scala.collection.JavaConversions._

class RecordingTrackModel(val channel: MidiChannel) {
  val channel_name = new SimpleStringProperty()
  val recording_enabled = new SimpleBooleanProperty()
  val recording_sessions_ol: ObservableList[RecordingSession] = FXCollections.observableArrayList[RecordingSession]
  val recording_sessions: ListProperty[RecordingSession] = new SimpleListProperty[RecordingSession](recording_sessions_ol)
  val recording_viewport: ObjectProperty[RecordingViewport] = new SimpleObjectProperty[RecordingViewport]()

  def getRecordingSessions: List[RecordingSession] = recording_sessions_ol.toList
  def setRecordingSessions(l: List[RecordingSession]): Unit = recording_sessions.setAll(l)
  def addRecordingSession(m: RecordingSession): Unit = recording_sessions_ol.add(m)
  def removeRecordingSession(m: RecordingSession): Unit = recording_sessions.remove(m)
  def clear(): Unit = recording_sessions_ol.clear()
  def getRecordingSessionsProperty: ListProperty[RecordingSession] = recording_sessions

  def getChannelName: String = channel_name.get
  def setChannelName(channelName: String): Unit = channel_name.set(channelName)
  def getChannelNameProperty: StringProperty = channel_name

  def isRecordingEnabled: Boolean = recording_enabled.get()
  def setRecordingEnabled(recordingEnabled: Boolean): Unit = recording_enabled.set(recordingEnabled)
  def getRecordingEnabledProperty: BooleanProperty = recording_enabled

  def getRecordingViewport: RecordingViewport = recording_viewport.get
  def setRecordingViewport(recordingViewport: RecordingViewport): Unit = recording_viewport.set(recordingViewport)
  def getRecordingViewportProperty: ObjectProperty[RecordingViewport] = recording_viewport
}

class RecordingTrack(parentController: ProjectSessionUpdating, channel: MidiChannel, model: RecordingTrackModel) extends BorderPane {
  private val _self = this

  val recordingTrackView = new RecordingTrackView()
  recordingTrackView.getPlaybackPositionProperty.bind(Context.playbackService.getPlaybackTimeProperty)
  recordingTrackView.getRecordingSessionsProperty.bind(model.getRecordingSessionsProperty)
  recordingTrackView.getRecordingViewportProperty.bind(model.getRecordingViewportProperty)

  @FXML var bpane_container: BorderPane = _
  @FXML var label_channel_name: Label = _
  @FXML var toggle_record: ToggleButton = _
  @FXML var button_clear: Button = _

  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/recording/RecordingTrack.fxml"))
  private val track = loader.load().asInstanceOf[BorderPane]
  this.setCenter(track)

  bpane_container.setCenter(recordingTrackView)

  def initialize(): Unit = {
    Context.globalRenderer.addSlave(recordingTrackView)
    label_channel_name.textProperty().bind(model.getChannelNameProperty)
    toggle_record.selectedProperty().bindBidirectional(model.getRecordingEnabledProperty)
  }
}
