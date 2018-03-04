package sound.audio.channel

import java.io.File
import javafx.beans.property.{ListProperty, SimpleListProperty}
import javafx.collections.{FXCollections, ObservableList}
import javax.sound.midi.{MidiMessage, ShortMessage}

import context.Context
import sound.audio.VstPlugin
import sound.midi.{MidiInterfaceIdentifier, MidiListener, MidiSubscriber}
import ui.controller.track.pianoRange.MidiEventSubscriber
import util.{KeyboardNote, MidiData}

import scala.collection.JavaConversions._

class MidiChannel(id: String, name: String) extends Channel {
  setId(id)
  setName(name)

  private var vstPlugin: Option[VstPlugin] = None
  private val midi_subscribers_ol: ObservableList[MidiEventSubscriber] = FXCollections.observableArrayList[MidiEventSubscriber]
  private val midi_subscribers: ListProperty[MidiEventSubscriber] = new SimpleListProperty[MidiEventSubscriber](midi_subscribers_ol)

  def getMidiSubscribers: List[MidiEventSubscriber] = midi_subscribers.toList
  def setMidiSubscribers(midiSubscribers: List[MidiEventSubscriber]): Unit = midi_subscribers.setAll(midiSubscribers)
  def addMidiSubscriber(midiSubscriber: MidiEventSubscriber): Unit = midi_subscribers.add(midiSubscriber)
  def removeMidiSubscriber(midiSubscriber: MidiEventSubscriber): Unit = midi_subscribers.remove(midiSubscriber)
  def getMidiSubscribersProperty: ListProperty[MidiEventSubscriber] = midi_subscribers

  def getVstPlugin: Option[VstPlugin] = vstPlugin

  def setVstSource(file: File) = {
    vstPlugin.foreach(_.close())
    vstPlugin = Some(new VstPlugin(file))
    vstPlugin.foreach(_.init())
  }

  def close(): Unit = {
    vstPlugin.foreach(_.close())
    vstPlugin = None
  }

  def pull(sampleRate: Double, bufferSize: Int): Array[Float] = {
    vstPlugin match {
      case Some(vst) =>
        vst.resetSampleInfo(sampleRate, bufferSize)
        vst.pull() match {
          case Some(data) =>
            data(0)
          case None =>
            Array.fill[Float](bufferSize)(0)
        }
      case None =>
        Array.fill[Float](bufferSize)(0)
    }
  }

  def queueMidiMessage(msg: ShortMessage) = {
    vstPlugin.foreach(_.queueMidiMessage(msg))
  }

  def stopListening() = {
    Context.midiService.unsubscribeOfAllInterfaces(getId)
  }

  def listen(midiInterfaceIdentifier: MidiInterfaceIdentifier) = {
    Context.midiService.addMidiSubscriber(midiInterfaceIdentifier, MidiSubscriber(getId, new MidiListener() {
      override def midiReceived(msg: MidiMessage, timeStamp: Long, sourceId: MidiInterfaceIdentifier): Unit = {
        msg match {
          case smsg: ShortMessage =>
            queueMidiMessage(msg.asInstanceOf[ShortMessage])
            if(smsg.getCommand == ShortMessage.NOTE_ON && smsg.getData2 > 0) {
              getMidiSubscribers.foreach(_.noteOn(KeyboardNote.widthAbsoluteIndex(smsg.getData1 - 12)))
            } else if(smsg.getCommand == ShortMessage.NOTE_OFF || (smsg.getCommand == ShortMessage.NOTE_ON && smsg.getData2 == 0)) {
              getMidiSubscribers.foreach(_.noteOff(KeyboardNote.widthAbsoluteIndex(smsg.getData1 - 12)))
            } else if(smsg.getCommand == ShortMessage.CONTROL_CHANGE && smsg.getData1 == MidiData.SUSTAIN_DAMPER_MIDI_DATA) {
              if(smsg.getData2 < 64) {
                getMidiSubscribers.foreach(_.sustainOff())
              } else {
                getMidiSubscribers.foreach(_.sustainOn())
              }
            }
          case _ =>
            println(s"Unknown MIDI message type [$msg] [${msg.getClass.getName}]")
        }
      }
    }))
  }
}
