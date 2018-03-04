package ui.controller.track

import javafx.beans.property.SimpleListProperty
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.{Button, ScrollPane}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.stage.{Modality, Stage}

import context.Context
import io.contracts._
import ui.controller.component.ProfileButton
import ui.controller.global.ProjectSessionUpdating
import ui.controller.monitor.{TrackProfile, TrackProfileInfo}
import ui.controller.{MainStageController, global}

import scala.collection.JavaConversions._
import scala.util.Random

class TrackSetModel {
  val track_set_ol: ObservableList[TrackModel] = FXCollections.observableArrayList[TrackModel]
  val track_set: SimpleListProperty[TrackModel] = new SimpleListProperty[TrackModel](track_set_ol)
  val track_profiles_ol: ObservableList[TrackProfile] = FXCollections.observableArrayList[TrackProfile]
  val tracks_profiles: SimpleListProperty[TrackProfile] = new SimpleListProperty[TrackProfile](track_profiles_ol)

  def getTrackProfiles: List[TrackProfile] = track_profiles_ol.toList
  def setTrackProfiles(l: List[TrackProfile]): Unit = track_profiles_ol.setAll(l)
  def addTrackProfiles(m: TrackProfile): Unit = track_profiles_ol.add(m)
  def removeTrackProfiles(m: TrackProfile): Unit = track_profiles_ol.remove(m)
  def getTrackProfilesProperty: SimpleListProperty[TrackProfile] = tracks_profiles
  
  def getTrackSet: List[TrackModel] = track_set_ol.toList
  def setTrackSet(l: List[TrackModel]): Unit = track_set.setAll(l)
  def addTrack(m: TrackModel): Unit = track_set_ol.add(m)
  def removeTrack(m: TrackModel): Unit = track_set_ol.remove(m)
  def clear(): Unit = track_set_ol.clear()
  def getTrackSetProperty: SimpleListProperty[TrackModel] = track_set
}

trait TrackSetController { _ : ProjectSessionUpdating =>
  @FXML var tracks: VBox = _
  @FXML var hbox_track_profiles: HBox = _
  @FXML var scrollpane_track_profiles: ScrollPane = _
  @FXML var button_add_track_profile: Button = _

  private val _self = this

  def initializeTrackSetController(mainController: MainStageController) = {
    Context.trackSetModel.getTrackProfilesProperty.addListener(new ListChangeListener[TrackProfile] {
      override def onChanged(c: Change[_ <: TrackProfile]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { busMixProfile =>
                val b = new ProfileButton(busMixProfile.name, busMixProfile.color)
                b.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
                  override def handle(event: MouseEvent): Unit = {
                    scrollpane_track_profiles.fireEvent(event)
                    hbox_track_profiles.fireEvent(event)
                  }
                })
                b.setOnAction(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    applyProfile(busMixProfile)
                  }
                })
                b.setOnDelete(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    Context.trackSetModel.removeTrackProfiles(busMixProfile)
                  }
                })
                b.setUserData(busMixProfile)
                hbox_track_profiles.getChildren.add(b)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .foreach { busChannelModel =>
                hbox_track_profiles.getChildren.find(_.getUserData == busChannelModel).foreach { c =>
                  hbox_track_profiles.getChildren.remove(c)
                }
              }
          }
        }
        updateProjectSession()
      }
    })

    Context.trackSetModel.getTrackSetProperty.addListener(new ListChangeListener[TrackModel] {
      override def onChanged(c: Change[_ <: TrackModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { trackModel =>
                val track = new TrackPanel(_self, trackModel.channel, trackModel)
                track.setUserData(trackModel)
                tracks.getChildren.add(track)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { trackModel =>
                tracks.getChildren.find(_.getUserData == trackModel) match {
                  case Some(track) =>
                    tracks.getChildren.remove(track)
                  case _ =>
                }
              }
          }
        }
        updateProjectSession()
      }
    })

    button_add_track_profile.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        import global.profileSave._

        val dialog = new Stage()
        val loader = new FXMLLoader()
        val m = new ProfileSaveModel()
        val controller = new ProfileSaveController(dialog, m)

        m.setProfileNames(Context.trackSetModel.getTrackProfiles.map(_.name))

        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/ProfileSaveDialog.fxml"))
        loader.setController(controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Save Profile")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if (m.getExitStatus == PROFILE_SAVE_MODAL_ACCEPT) {
          def trackProfileFromModel = {
            val r = new Random(m.getResultName.hashCode)
            TrackProfile(
              name = m.getResultName,
              color = new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1).desaturate(),
              tracks = Context.trackSetModel.getTrackSet.map { trackModel =>
                TrackProfileInfo(
                  id = trackModel.channel.getId,
                  midiInput = Option(trackModel.getSelectedMidiInterface),
                  vstInput = Option(trackModel.getSelectedMidiVst),
                  vstProperties = extractProperties(trackModel),
                  pianoEnabled = trackModel.getTrackPianoEnabled(),
                  pianoRollEnabled = trackModel.getTrackPianoRollEnabled(),
                  pianoRangeStart = trackModel.getTrackPianoStartNote,
                  pianoRangeEnd = trackModel.getTrackPianoEndNote
                )
              }
            )
          }

          m.getResultAction match {
            case ProfileSaveAction.OVERRIDE =>
              Context.trackSetModel.getTrackProfiles.find(_.name == m.getResultName).foreach { removeTrackProfile =>
                Context.trackSetModel.removeTrackProfiles(removeTrackProfile)
              }
              Context.trackSetModel.addTrackProfiles(trackProfileFromModel)
            case ProfileSaveAction.NEW =>
              Context.trackSetModel.addTrackProfiles(trackProfileFromModel)
            case _ =>
          }
        }
      }
    })
  }

  def linkAllMidiDevices() = {
    tracks.getChildren.foreach { track => track.asInstanceOf[TrackPanel].linkMidiDeviceModal(true) }
  }

  def applyProfile(p: TrackProfile): Unit = {
    p.tracks.foreach { trackProfileInfo =>
      Context.trackSetModel.getTrackSet.find(_.channel.getId == trackProfileInfo.id).foreach { trackModel =>
        trackProfileInfo.midiInput.foreach { profileMidiInput =>
          if(trackModel.getMidiInterfaceNames.contains(profileMidiInput)) {
            trackModel.setSelectedMidiInterface(profileMidiInput)
          }
        }
        trackProfileInfo.vstInput.foreach { profileVstInput =>
          if(trackModel.getMidiVstSources.contains(profileVstInput)) {
            trackModel.setSelectedMidiVst(profileVstInput)
          }
        }
        trackProfileInfo.vstProperties.foreach { profileVstProperties =>
          profileVstProperties.foreach { case (key, value) =>
            trackModel.channel.getVstPlugin.flatMap(_.vst).foreach { vst =>
              vst.setParameter(key.toInt, value.toFloat)
            }
          }
        }
        trackModel.setTrackPianoEnabled(trackProfileInfo.pianoEnabled)
        trackModel.setTrackPianoRollEnabled(trackProfileInfo.pianoRollEnabled)
        trackModel.setTrackPianoStartNote(trackProfileInfo.pianoRangeStart)
        trackModel.setTrackPianoEndNote(trackProfileInfo.pianoRangeEnd)
      }
    }
  }

  def getTrackSession(): SaveTracks =
    SaveTracks(
      `channel-info` = Context.trackSetModel.getTrackSet.map { track =>
        SaveChannelInfo(
          `id` = track.channel.getId,
          `name` = track.getTrackName,
          `midi-input` = Option(track.getSelectedMidiInterface).map(_.name),
          `vst-i` = Option(track.getSelectedMidiVst).map(_.path),
          `vst-properties` = extractProperties(track),
          `piano-enabled` = track.getTrackPianoEnabled(),
          `piano-roll-enabled` = track.getTrackPianoRollEnabled(),
          `piano-range-start` = SavePianoRange(
            `note`= track.getTrackPianoStartNote.note.toString,
            `index` = track.getTrackPianoStartNote.index
          ),
          `piano-range-end` = SavePianoRange(
            `note`= track.getTrackPianoEndNote.note.toString,
            `index` = track.getTrackPianoEndNote.index
          )
        )
      },
      `channel-profiles` = Context.trackSetModel.getTrackProfiles.map { trackProfile =>
        SaveChannelProfile(
          `name` = trackProfile.name,
          `color`= util.colorToWebHex(trackProfile.color),
          `channel-profiles` = trackProfile.tracks.map { trackProfileInfo =>
            SaveChannelProfileInfo(
              `id` = trackProfileInfo.id,
              `midi-input` = trackProfileInfo.midiInput.map { trackProfileMidiInput =>
                SaveChannelProfileMidiInput(
                  `name` = trackProfileMidiInput.name
                )
              },
              `vst-input` = trackProfileInfo.vstInput.map { trackProfileVstInput =>
                SaveChannelProfileVstInput(
                  `path` = trackProfileVstInput.path,
                  `name` = trackProfileVstInput.name
                )
              },
              `vst-properties` = trackProfileInfo.vstProperties,
              `piano-enabled` = trackProfileInfo.pianoEnabled,
              `piano-roll-enabled` = trackProfileInfo.pianoRollEnabled,
              `piano-range-start` = SavePianoRange(
                `note`= trackProfileInfo.pianoRangeStart.note.toString,
                `index` = trackProfileInfo.pianoRangeStart.index
              ),
              `piano-range-end` = SavePianoRange(
                `note`= trackProfileInfo.pianoRangeEnd.note.toString,
                `index` = trackProfileInfo.pianoRangeEnd.index
              )
            )
          }
        )
      }
    )

  private def extractProperties(t: TrackModel) = {
    t.channel.getVstPlugin.flatMap { vst =>
      vst.vst.map { v =>
        val n = v.numParameters()
        (0 until n).map { pIndex =>
          pIndex.toString -> v.getParameter(pIndex).toDouble
        }.toMap
      }
    }
  }
}
