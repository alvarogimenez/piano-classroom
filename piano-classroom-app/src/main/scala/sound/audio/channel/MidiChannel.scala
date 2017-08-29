package sound.audio.channel

import java.io.File
import javax.sound.midi.ShortMessage

import sound.audio.VstPlugin
import sound.audio.mixer.MixerUtils


class MidiChannel(val index: Int) extends Channel {
  var vstPlugin: Option[VstPlugin] = None

  def setVstSource(file: File) = {
    vstPlugin.foreach(_.close())
    vstPlugin = Some(new VstPlugin(file))
    vstPlugin.foreach(_.init())
  }

  def pull(sampleRate: Double, bufferSize: Int): Array[Float] = {
//    println(s"MIDI Channel pull ($index) with SampleRate [$sampleRate], BufferSize [$bufferSize]")
    vstPlugin match {
      case Some(vst) =>
        vst.resetSampleInfo(sampleRate, bufferSize)
        vst.pull() match {
          case Some(data) =>
            data.reduce((a, b) => MixerUtils.mix(a, b, 1f, 1f))
          case None =>
            println(s"VST Plugin Not Created in MIDI Channel ($index). Sending Zero buffer. ")
            Array.fill[Float](bufferSize)(0)
        }
      case None =>
        println(s"VST Plugin Not Initialized in MIDI Channel ($index). Sending Zero buffer. ")
        Array.fill[Float](bufferSize)(0)
    }
  }

  def queueMidiMessage(msg: ShortMessage) = {
    println(s"Queue message on MIDI Channel ($index) [$msg]")
    vstPlugin.foreach(_.queueMidiMessage(msg))
  }
}
