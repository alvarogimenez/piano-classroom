package ui.controller.track

import java.util.UUID
import javafx.beans.property.SimpleListProperty
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.fxml.FXML
import javafx.scene.layout.VBox

import context.Context
import sound.audio.channel.MidiChannel

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

trait TrackSetController {
  @FXML var tracks: VBox = _

  def initializeTrackSetController() = {
    Context.trackSetModel.getTrackSetProperty.addListener(new ListChangeListener[TrackModel] {
      override def onChanged(c: Change[_ <: TrackModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { trackModel =>
                val track = new TrackPanel(trackModel.channel, trackModel)
                track.setUserData(trackModel)
                tracks.getChildren.add(track)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { trackModel =>
                tracks.getChildren.remove(tracks.getChildren.find(_.getUserData == trackModel))
              }
          }
        }
      }
    })
  }
}
