package sound.audio.channel

import java.io.File
import javax.sound.midi.ShortMessage

import sound.audio.VstPlugin
import sound.audio.mixer.MixerUtils


class MidiChannel(val id: String) extends Channel {
  var vstPlugin: Option[VstPlugin] = None

  def setVstSource(file: File) = {
    vstPlugin.foreach(_.close())
    vstPlugin = Some(new VstPlugin(file))
    vstPlugin.foreach(_.init())
  }

  def pull(sampleRate: Double, bufferSize: Int): Array[Float] = {
    vstPlugin match {
      case Some(vst) =>
        vst.resetSampleInfo(sampleRate, bufferSize)
        vst.pull() match {
          case Some(data) =>
            data.reduce((a, b) => MixerUtils.mix(a, b, 1f, 1f))
          case None =>
            Array.fill[Float](bufferSize)(0)
        }
      case None =>
        Array.fill[Float](bufferSize)(0)
    }
  }

  def queueMidiMessage(msg: ShortMessage) = {
    println(s"Queue message on MIDI Channel ($id) [$msg]")
    vstPlugin.foreach(_.queueMidiMessage(msg))
  }
}
