package ui.controller.recording

import java.lang.Boolean
import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty, SimpleListProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, TextField, ToggleButton}
import javafx.scene.layout.{Pane, VBox}
import javafx.scene.paint.Color
import javax.sound.midi.ShortMessage

import context.Context
import services.audio.playback.{PlaybackNoteOffEvent, PlaybackNoteOnEvent, PlaybackSustainOffEvent, PlaybackSustainOnEvent}
import ui.controller.MainStageController
import ui.controller.global.ProjectSessionUpdating
import ui.controller.track.pianoRange.MidiEventSubscriber
import util.{KeyboardNote, MidiData, MusicNote}

import scala.collection.JavaConversions._
import scala.util.Random

class RecordingModel {
  val recording_tracks_ol: ObservableList[RecordingTrackModel] = FXCollections.observableArrayList[RecordingTrackModel]
  val recording_tracks: SimpleListProperty[RecordingTrackModel] = new SimpleListProperty[RecordingTrackModel](recording_tracks_ol)
  val playing: BooleanProperty = new SimpleBooleanProperty()
  
  def getRecordingTracks: List[RecordingTrackModel] = recording_tracks_ol.toList
  def setRecordingTracks(l: List[RecordingTrackModel]): Unit = recording_tracks.setAll(l)
  def addRecordingTrack(m: RecordingTrackModel): Unit = recording_tracks_ol.add(m)
  def removeRecordingTrack(m: RecordingTrackModel): Unit = recording_tracks_ol.remove(m)
  def clear(): Unit = recording_tracks_ol.clear()
  def getRecordingTracksProperty: SimpleListProperty[RecordingTrackModel] = recording_tracks
  
  def isPlaying: Boolean = playing.get()
  def setPlaying(p: Boolean) = playing.set(p)
  def getPlayingProperty: BooleanProperty = playing
}

trait RecordingController { _ : ProjectSessionUpdating =>
  private val _self = this
  private val _model = Context.recordingModel
  
  @FXML var vbox_recording_tracks: VBox = _
  @FXML var textfield_last_open_file: TextField = _
  @FXML var button_save_file: Button = _
  @FXML var button_save_file_as: Button = _
  @FXML var button_open_file: Button = _
  @FXML var button_record_skip_left: Button = _
  @FXML var button_record_skip_right: Button = _
  @FXML var button_record_midi_append: ToggleButton = _
  @FXML var button_stop: Button = _
  @FXML var button_play: Button = _
  @FXML var button_record: ToggleButton = _
  @FXML var toggle_metronome_enabled: ToggleButton = _
  @FXML var pane_metronome_view: Pane = _
  @FXML var button_metronome_tap: Button = _
  @FXML var button_metronome_bar: Button = _

  def initializeRecordingController(mainController: MainStageController) = {
    _model.getRecordingTracksProperty.addListener(new ListChangeListener[RecordingTrackModel] {
      override def onChanged(c: Change[_ <: RecordingTrackModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { trackModel =>
                val track = new RecordingTrack(_self, trackModel.channel, trackModel)
                val data =
                  (0 to 30).map { x =>
                    RecordingNoteData(x * 247, Some(x * 247 + 247), KeyboardNote.widthAbsoluteIndex(x + 20))
                  }
                val session = new RecordingSession(Color.LIGHTCORAL)
                session.setStart(0)
                session.setEnd(Some(10000))
                session.setData(data.toList)

                trackModel.setRecordingSessions(List(session))

                track.setUserData(trackModel)
                vbox_recording_tracks.getChildren.add(track)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { trackModel =>
                vbox_recording_tracks.getChildren.find(_.getUserData == trackModel) match {
                  case Some(track) =>
                    vbox_recording_tracks.getChildren.remove(track)
                  case _ =>
                }
              }
          }
        }
      }
    })

    button_play.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        if(_model.isPlaying) {
          midiOff()
          Context.playbackService.pause()
          _model.setPlaying(false)
          button_play.setStyle("-fx-graphic: url('assets/icon/RecordingPlay.png')")
          if(button_record.isSelected) {
            stopRecording()
          }
        } else {
          val events = _model.getRecordingTracks.flatMap { recordingTrack =>
            if(!button_record.isSelected || button_record_midi_append.isSelected) {
              recordingTrack.getRecordingSessions.flatMap { recordingSession =>
                recordingSession.getData.flatMap {
                  case data: RecordingNoteData =>
                    List(
                      PlaybackNoteOnEvent(data.start, data.note, recordingTrack.channel)
                    ) ++ data.end.map { e => PlaybackNoteOffEvent(e, data.note, recordingTrack.channel) }.toList
                  case data: RecordingSustainData =>
                    List(
                      PlaybackSustainOnEvent(data.start, recordingTrack.channel)
                    ) ++ data.end.map { e => PlaybackSustainOffEvent(e, recordingTrack.channel) }.toList
                }
              }
            } else {
              List.empty
            }
          }

          Context.playbackService.play(events)
          _model.setPlaying(true)
          button_play.setStyle("-fx-graphic: url('assets/icon/RecordingPause.png')")

          if(button_record.isSelected) {
            startRecording()
          }
        }
      }
    })

    button_stop.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        midiOff()
        Context.playbackService.stop()
        _model.setPlaying(false)
        button_play.setStyle("-fx-graphic: url('assets/icon/RecordingPlay.png')")
        
        if(button_record.isSelected) {
          stopRecording()
        }
        button_record.setSelected(false)
      }
    })
    
    button_record.selectedProperty().addListener(new ChangeListener[java.lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: java.lang.Boolean], oldValue: java.lang.Boolean, newValue: java.lang.Boolean): Unit = {
        if(newValue && _model.isPlaying) {
          startRecording()
        } else if(!newValue) {
          stopRecording()
        }
      }
    })
  }

  case class SessionInfo(
    recordingSession: RecordingSession,
    midiSubscriber: MidiEventSubscriber
  )
  case class RecordingInfo(
    recordingStart: Long,
    sessionInfo: Map[String, SessionInfo]
  )

  var _currentSessionInfo: Option[RecordingInfo] = None
  
  private def startRecording() = {
    val playbackStart = Context.playbackService.getPlaybackTime
    val recordingStart = System.currentTimeMillis() - playbackStart

    val recordingTracks = _model.getRecordingTracks.filter(_.isRecordingEnabled)
    val sessionsByChannel =
      recordingTracks.map { recordingTrack =>
        val session = new RecordingSession(new Color(Random.nextDouble(), Random.nextDouble(), Random.nextDouble(), 1).desaturate())
        session.setStart(playbackStart)
        session.setEnd(None)
        session.setRecordingActive(true)

        recordingTrack.channel.getId -> SessionInfo(
          session,
          new MidiEventSubscriber {
            override def sustainOn() =
              if(_currentSessionInfo.isDefined) {
                session.addData(RecordingSustainData(System.currentTimeMillis() - recordingStart, None))
              }

            override def sustainOff() =
              if(_currentSessionInfo.isDefined) {
                val lastOpenNote = session.getData.collectFirst {case e: RecordingSustainData if e.end.isEmpty => e }
                if(lastOpenNote.isDefined) {
                  session.removeData(lastOpenNote.get)
                  session.addData(lastOpenNote.get.copy(end = Some(System.currentTimeMillis() - recordingStart)))
                }
              }

            override def noteOn(kn: KeyboardNote) =
              if(_currentSessionInfo.isDefined) {
                session.addData(RecordingNoteData(System.currentTimeMillis() - recordingStart, None, kn))
              }

            override def noteOff(kn: KeyboardNote) = {
              if(_currentSessionInfo.isDefined) {
                val lastOpenNote = session.getData.collectFirst {case e: RecordingNoteData if e.end.isEmpty => e }
                if(lastOpenNote.isDefined) {
                  session.removeData(lastOpenNote.get)
                  session.addData(lastOpenNote.get.copy(end = Some(System.currentTimeMillis() - recordingStart)))
                }
              }
            }
          }
        )

      }.toMap

    recordingTracks.foreach { recordingTrack =>
      recordingTrack.addRecordingSession(sessionsByChannel(recordingTrack.channel.getId).recordingSession)
      recordingTrack.channel.addMidiSubscriber(sessionsByChannel(recordingTrack.channel.getId).midiSubscriber)
    }

    _currentSessionInfo = Some(RecordingInfo(
      recordingStart = recordingStart,
      sessionInfo = sessionsByChannel
    ))
  }

  private def stopRecording() = {
    _currentSessionInfo match {
      case Some(currentSessionInfo) =>
        currentSessionInfo.sessionInfo.toList.foreach {
          case (channelId, sessionInfo) =>
            val newSession = sessionInfo.recordingSession
            val newSessionEnd = System.currentTimeMillis() - currentSessionInfo.recordingStart
            newSession.setEnd(Some(newSessionEnd))
            newSession.setRecordingActive(false)

            val recordingTrack = _model.getRecordingTracks.find(_.channel.getId == channelId)
            if(button_record_midi_append.isSelected) {
              newSession.setData(
                recordingTrack.toList.flatMap { recordingTrack =>
                  recordingTrack.getRecordingSessions.flatMap(_.getData).filter { data =>
                    newSession.getEnd.exists(_ > data.start) && data.end.forall(_ > newSession.getStart)
                  }
                } ++ newSession.getData
              )
            }

            recordingTrack.foreach { r =>
              r.channel.removeMidiSubscriber(sessionInfo.midiSubscriber)

              r.getRecordingSessions.filterNot(_ == newSession).foreach { recordingSession =>
                if(recordingSession.getStart < newSession.getStart && recordingSession.getEnd.forall(_ > newSession.getStart)) {
                  recordingSession.setEnd(Some(newSession.getStart))
                  recordingSession.setData(recordingSession.getData.filter(_.start < newSession.getStart))
                } else if((recordingSession.getStart < newSessionEnd) && recordingSession.getEnd.forall(_ > newSessionEnd)) {
                  recordingSession.setStart(newSession.getEnd.get)
                  recordingSession.setData(recordingSession.getData.filter(_.start > newSession.getEnd.get))
                } else if(recordingSession.getStart >= newSession.getStart && recordingSession.getEnd.forall(_ <= newSessionEnd)) {
                  r.removeRecordingSession(recordingSession)
                }
              }
            }
        }

        _currentSessionInfo = None
      case _ =>
    }
  }

  private def midiOff() = {
    _model.getRecordingTracks.map(_.channel).foreach { channel =>
      (0 to 127)
        .foreach { note =>
          val msg = new ShortMessage(ShortMessage.NOTE_OFF, note, 0x00)
          channel.queueMidiMessage(msg)
        }
      val msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, MidiData.SUSTAIN_DAMPER_MIDI_DATA, 0x00)
      channel.queueMidiMessage(msg)
    }
  }
}
