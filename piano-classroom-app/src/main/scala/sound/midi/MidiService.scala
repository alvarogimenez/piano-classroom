package sound.midi

import java.io.File
import javax.sound.midi._

import context.Context

import scala.collection.JavaConversions._

class MidiService {
  private class MidiInputReceiver(sourceId: MidiInterfaceIdentifier, source: MidiDevice) extends Receiver {
    override def send(msg: MidiMessage, timeStamp: Long): Unit = {
      midiSubscriber.getOrElse(sourceId, List.empty)
        .foreach { subscriber =>
          subscriber.listener.midiReceived(msg, timeStamp)
        }
    }
    override def close(): Unit = {}
  }

  var midiSubscriber: Map[MidiInterfaceIdentifier, List[MidiSubscriber]] = Map.empty

  def addMidiSubscriber(id: MidiInterfaceIdentifier, subscriber: MidiSubscriber): Unit = {
    midiSubscriber = midiSubscriber.updated(id, midiSubscriber.getOrElse(id, List.empty) :+ subscriber)
  }

  def removeMidiSubscriber(id: MidiInterfaceIdentifier, subscriber: MidiSubscriber): Unit = {
    midiSubscriber = midiSubscriber.updated(id, midiSubscriber.getOrElse(id, List.empty).filter(_ == subscriber))
  }

  def unsubscribeOfAllInterfaces(id: String): Unit = {
    midiSubscriber =
      midiSubscriber
        .map { case (key, value) =>
          (key, value.filterNot(_.id == id))
        }
  }

  def getHardwareMidiDevices: Map[MidiInterfaceIdentifier, MidiDevice] = {
    MidiSystem.getMidiDeviceInfo
        .map(MidiSystem.getMidiDevice)
        .filter(device => !device.isInstanceOf[Sequencer] && !device.isInstanceOf[Synthesizer])
        .groupBy(_.getDeviceInfo.getName)
        .toList
        .flatMap { case (name, devices) =>
          devices.toList match {
            case d :: Nil => List(MidiInterfaceIdentifier(name) -> d)
            case _ =>
              devices
                .zipWithIndex
                .map { case (device, index) =>
                  MidiInterfaceIdentifier(s"[$name]:$index") -> device
                }
          }
        }
        .toMap
  }

  def getVstSources: List[File] = {
    Context.sessionSettings.`vst-configuration`.`vst-source-directories`
        .flatMap { directory =>
          val fDir = new File(directory)
          if(fDir.exists() && fDir.isDirectory) {
            fDir
            .listFiles()
              .toList
              .filter(f => f.isFile && f.getName.endsWith(".dll"))
          } else {
            List.empty
          }
        }
  }

  def attach() = {
    getHardwareMidiDevices
      .foreach { case (id, device) =>
        try {
          device.getTransmitter.setReceiver(new MidiInputReceiver(id, device))
          device.open()
        } catch {
          case e: MidiUnavailableException =>
            println(s"Error opening [$id]: '${e.getMessage}'")
        }
      }
  }

  def detach() = {
    getHardwareMidiDevices
      .foreach { case (id, device) =>
        try {
          device.getTransmitters.toList.foreach(_.close())
          device.close()
        } catch {
          case e: MidiUnavailableException =>
            println(s"Error closing [$id]: '${e.getMessage}'")
        }
      }
  }
}
