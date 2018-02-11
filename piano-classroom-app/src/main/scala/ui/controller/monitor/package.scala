package ui.controller

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

import sound.midi.{MidiInterfaceIdentifier, MidiVstSource}
import util.KeyboardNote


package object monitor {
  case class TrackProfile(
    name: String,
    color: Color,
    tracks: List[TrackProfileInfo]
  )

  case class TrackProfileInfo(
    id: String,
    midiInput: Option[MidiInterfaceIdentifier],
    vstInput: Option[MidiVstSource],
    vstProperties: Option[Map[String, Double]],
    pianoEnabled: Boolean,
    pianoRollEnabled: Boolean,
    pianoRangeStart: KeyboardNote,
    pianoRangeEnd: KeyboardNote
  )

  case class ChannelSource(
    name: String,
    id: String
  ) {
    override def toString(): String = name
  }

  case class WebCamSource(
    name: String,
    index: Int
  ) {
    override def toString(): String = name
  }

  case class GraphicsDecorator(
    decorator: (GraphicsContext, Rectangle) => Unit
  )

  object MonitorSource extends Enumeration {
    type MonitorSource = Value
    val CAMERA, PENCIL, BOARD, MUSIC = Value
  }
}
