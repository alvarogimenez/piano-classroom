package sound.midi

import javax.sound.midi._
import scala.collection.JavaConversions._

class MidiController {
  private class MidiInputReceiver(sourceId: MidiInterfaceIdentifier, source: MidiDevice) extends Receiver {
    override def send(msg: MidiMessage, timeStamp: Long): Unit = {
      println(s"Received MIDI [${msg.getMessage}] from [$sourceId]")
      midiListeners.getOrElse(sourceId, List.empty)
        .foreach { listener =>
          listener.midiReceived(msg, timeStamp)
        }
    }
    override def close(): Unit = {}
  }

  var midiListeners: Map[MidiInterfaceIdentifier, List[MidiListener]] = Map.empty

  def addMidiListener(id: MidiInterfaceIdentifier, listener: MidiListener) = {
    midiListeners = midiListeners.updated(id, midiListeners.getOrElse(id, List.empty) :+ listener)
  }

  def removeMidiListener(id: MidiInterfaceIdentifier, listener: MidiListener) = {
    midiListeners = midiListeners.updated(id, midiListeners.getOrElse(id, List.empty).filter(_ == listener))
  }

  def removeMidiListener(listener: MidiListener) = {
    midiListeners =
      midiListeners
        .map { case (key, value) =>
          (key, midiListeners.getOrElse(key, List.empty).filterNot(_ == listener))
        }
  }

  def getHardwareMidiDevices(): Map[MidiInterfaceIdentifier, MidiDevice] = {
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

  def attach() = {
    getHardwareMidiDevices()
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
    getHardwareMidiDevices()
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
