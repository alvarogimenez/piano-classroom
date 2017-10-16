package sound.midi

import java.io.File
import javafx.concurrent.Task
import javax.sound.midi._

import context.Context

import scala.collection.JavaConversions._
import scala.util.Random

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

  val task = new Task[Unit]() {
    override def call(): Unit = {
      while(!isCancelled) {
//        val sustainOn = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0x40, 127)
//        midiSubscriber
//          .values
//          .flatten
//          .foreach { s =>
//            s.listener.midiReceived(sustainOn, System.currentTimeMillis())
//          }
        (48 to 72)
          .foreach { i =>
            val msgOn = new ShortMessage(ShortMessage.NOTE_ON, i, 64)
            val msgOff = new ShortMessage(ShortMessage.NOTE_OFF, i, 0)
            midiSubscriber
            .values
              .flatten
              .foreach { s =>
                s.listener.midiReceived(msgOn, System.currentTimeMillis())
              }
            Thread.sleep(200)
            midiSubscriber
              .values
              .flatten
              .foreach { s =>
                s.listener.midiReceived(msgOff, System.currentTimeMillis())
              }
          }
//        val sustainOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0x40, 0)
//        midiSubscriber
//          .values
//          .flatten
//          .foreach { s =>
//            s.listener.midiReceived(sustainOff, System.currentTimeMillis())
//          }
      }
    }
  }
  val thread = new Thread(task)
  thread.setDaemon(true)
  thread.start()

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
