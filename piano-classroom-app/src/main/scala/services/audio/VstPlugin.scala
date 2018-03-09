package services.audio

import java.io.File
import javax.sound.midi.ShortMessage

import com.synthbot.audioplugin.vst.vst2.JVstHost2

import scala.util.Try


class VstPlugin(val file: File) {
  var vst: Option[JVstHost2] = None

  def init() = {
    vst =
      Try(JVstHost2.newInstance(file))
          .recover {
            case e : Exception =>
              println(s"Exception while opening VST '${file.getPath}': '${e.getMessage}'")
              throw e
          }
          .toOption
  }

  def close() = {
    try {
      vst.foreach(_.turnOffAndUnloadPlugin())
    } catch {
      case e: Exception =>
        println(s"Exception while closing VST '${file.getPath}': '${e.getMessage}'")
    }
  }

  def resetSampleInfo(sampleRate: Double, bufferSize: Int) = {
    vst match {
      case Some(v) =>
        if(v.getSampleRate != sampleRate || v.getBlockSize != bufferSize) {
          println(s"Reset VST controller")
          v.setSampleRate(sampleRate.toFloat)
          v.setBlockSize(bufferSize)
          v.turnOff()
          v.turnOn()
        }
    }
  }

  def queueMidiMessage(msg: ShortMessage) = {
    vst.foreach(_.queueMidiMessage(msg))
  }

  def openPluginEditor(source: String) = {
    vst.foreach(_.openEditor(s"Editor for '$source'"))
  }

  def pull(): Option[Array[Array[Float]]] = {
//    println(s"VST pull [${vst.isDefined}]")
    vst.map { vst =>
      val output = Array.ofDim[Float](vst.numOutputs, vst.getBlockSize)
      vst.processReplacing(Array.empty, output, vst.getBlockSize)
//      println(output.map(_.size).mkString(","))
      output
    }
  }
}
