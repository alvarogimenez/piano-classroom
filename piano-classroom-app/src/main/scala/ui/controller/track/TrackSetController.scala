package ui.controller.track

import javafx.beans.property.SimpleListProperty
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.fxml.FXML
import javafx.scene.layout.VBox

import context.Context
import io.contracts.{SaveChannelInfo, SavePianoRange, SaveTracks}
import ui.controller.{MainStageController, ProjectSessionUpdating}

import scala.collection.JavaConversions._

class TrackSetModel {
  val track_set_ol: ObservableList[TrackModel] = FXCollections.observableArrayList[TrackModel]
  val track_set: SimpleListProperty[TrackModel] = new SimpleListProperty[TrackModel](track_set_ol)

  def getTrackSet: List[TrackModel] = track_set_ol.toList
  def setTrackSet(l: List[TrackModel]): Unit = track_set.setAll(l)
  def addTrack(m: TrackModel): Unit = track_set_ol.add(m)
  def removeTrack(m: TrackModel): Unit = track_set_ol.remove(m)
  def clear(): Unit = track_set_ol.clear()
  def getTrackSetProperty: SimpleListProperty[TrackModel] = track_set
}

trait TrackSetController { _ : ProjectSessionUpdating =>
  @FXML var tracks: VBox = _
  private val _self = this

  def initializeTrackSetController(mainController: MainStageController) = {
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
  }

  def getTrackSession(): SaveTracks =
    SaveTracks(
      `channel-info` = Context.trackSetModel.getTrackSet.map { track =>
        SaveChannelInfo(
          `id` = track.channel.id,
          `name` = track.getTrackName,
          `midi-input` = Option(track.getSelectedMidiInterface).map(_.name),
          `vst-i` = Option(track.getSelectedMidiVst),
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
      }
    )
}
