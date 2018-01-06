package ui.controller.track.midiLink

import java.util.UUID
import javafx.application.Platform
import javafx.beans.property.{SimpleBooleanProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, CheckBox, Label}
import javafx.stage.Stage
import javax.sound.midi.{MidiMessage, ShortMessage}

import context.Context
import sound.midi.{MidiInterfaceIdentifier, MidiListener, MidiSubscriber}

class MidiLinkModel {
  val auto_close_modal: SimpleBooleanProperty = new SimpleBooleanProperty()
  val last_midi_event: SimpleStringProperty = new SimpleStringProperty()
  val last_midi_source: SimpleObjectProperty[MidiInterfaceIdentifier] = new SimpleObjectProperty[MidiInterfaceIdentifier]()
  
  var exit_status: Int = _

  def getAutoCloseModal: Boolean = auto_close_modal.get()
  def setAutoCloseModal(a: Boolean): Unit = auto_close_modal.set(a)
  def getAutoCloseModalProperty: SimpleBooleanProperty = auto_close_modal

  def getLastMidiEvent: String = last_midi_event.get()
  def setLastMidiEvent(a: String): Unit = last_midi_event.set(a)
  def getLastMidiEventProperty: SimpleStringProperty = last_midi_event

  def getLastMidiSource: MidiInterfaceIdentifier = last_midi_source.get()
  def setLastMidiSource(a: MidiInterfaceIdentifier): Unit = last_midi_source.set(a)
  def getLastMidiSourceProperty: SimpleObjectProperty[MidiInterfaceIdentifier] = last_midi_source

  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  setAutoCloseModal(false)
}

class MidiLinkController(dialog: Stage, model: MidiLinkModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var checkbox_auto_close: CheckBox = _
  @FXML var label_last_event: Label = _
  @FXML var label_last_source: Label = _

  val subscriber = MidiSubscriber(
    id = UUID.randomUUID().toString,
    listener = new MidiListener {
      override def midiReceived(msg: MidiMessage, timeStamp: Long, source: MidiInterfaceIdentifier) = {
        msg match {
          case smsg: ShortMessage =>
            if (smsg.getCommand == ShortMessage.NOTE_ON && smsg.getData2 > 0) {
              Platform.runLater(new Runnable() {
                def run(): Unit = {
                  model.setLastMidiSource(source)
                  model.setLastMidiEvent("CMD: %x DATA1: %x DATA2: %x".format(smsg.getCommand, smsg.getData1, smsg.getData2))
                  if(model.getAutoCloseModal) {
                    model.setExitStatus(MIDI_LINK_MODAL_ACCEPT)
                    endSubscription()
                    dialog.close()
                  }
                }
              })
            }
          case _ =>
        }
      }
    }
  )

  def initialize() : Unit = {
    Context.midiService.getHardwareMidiDevices.foreach { case (id, _) =>
      Context.midiService.addMidiSubscriber(id, subscriber)
    }

    checkbox_auto_close.selectedProperty().bindBidirectional(model.getAutoCloseModalProperty) 
    
    label_last_event.textProperty().bind(model.getLastMidiEventProperty)
    label_last_source.textProperty().bind(model.getLastMidiSourceProperty.asString())
    
    button_cancel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(MIDI_LINK_MODAL_CANCEL)
        endSubscription()
        dialog.close()
      }
    })

    button_accept.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(MIDI_LINK_MODAL_ACCEPT)
        endSubscription()
        dialog.close()
      }
    })
  }

  def endSubscription(): Unit = {
    Context.midiService.unsubscribeOfAllInterfaces(subscriber.id)
  }
}
