package services.audio

import services.audio.channel.MidiChannel
import util.KeyboardNote

package object playback {
  trait PlaybackEvent {
    val time: Long
  }

  case class PlaybackNoteOnEvent(time: Long, note: KeyboardNote, channel: MidiChannel) extends PlaybackEvent
  case class PlaybackNoteOffEvent(time: Long, note: KeyboardNote, channel: MidiChannel) extends PlaybackEvent
  case class PlaybackSustainOnEvent(time: Long, channel: MidiChannel) extends PlaybackEvent
  case class PlaybackSustainOffEvent(time: Long, channel: MidiChannel) extends PlaybackEvent

  case class PlaybackStage(time: Long, events: List[PlaybackEvent])
}
