package sound.midi

import javax.sound.midi.MidiMessage

trait MidiListener {
  def midiReceived(msg: MidiMessage, timeStamp: Long, source: MidiInterfaceIdentifier)
}
