import javafx.scene.paint.Color

package object util {
  object MidiData {
    final val SUSTAIN_DAMPER_MIDI_DATA = 0x40
  }

  trait NoteStatus
  case object NoteActive extends NoteStatus
  case object NoteSustained extends NoteStatus

  def colorToWebHex(c: Color) = {
    "#%02X%02X%02X".format((c.getRed * 255).toInt, (c.getGreen * 255).toInt, (c.getBlue * 255).toInt)
  }
}
