package ui.controller.track

import java.io.File
import javafx.beans.property.{Property, SimpleListProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, Event, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{Button, ComboBox, Tooltip}
import javafx.scene.input.{ContextMenuEvent, MouseEvent}
import javafx.scene.layout.BorderPane
import javax.sound.midi.{MidiMessage, ShortMessage}

import context.Context
import sound.audio.channel.MidiChannel
import sound.midi.{MidiInterfaceIdentifier, MidiListener}

import scala.collection.JavaConversions._

case class TrackPanelInitialSettings(
  midiIn: MidiInterfaceIdentifier,
  vstSource: String
)

class TrackModel() {
  var track_name = new SimpleStringProperty()
  val midi_interface_names_ol: ObservableList[MidiInterfaceIdentifier] = FXCollections.observableArrayList[MidiInterfaceIdentifier]
  val midi_interface_names: SimpleListProperty[MidiInterfaceIdentifier] = new SimpleListProperty[MidiInterfaceIdentifier](midi_interface_names_ol)
  val midi_vst_source_names_ol: ObservableList[String] = FXCollections.observableArrayList[String]
  val midi_vst_source_names: SimpleListProperty[String] = new SimpleListProperty[String](midi_vst_source_names_ol)
  val selected_midi_interface: Property[MidiInterfaceIdentifier] = new SimpleObjectProperty[MidiInterfaceIdentifier]()
  val selected_midi_vst: SimpleStringProperty = new SimpleStringProperty()

  def getTrackName: String = track_name.get
  def setTrackName(t: String): Unit = track_name.set(t)
  def getTrackNameProperty: SimpleStringProperty = track_name

  def getMidiInterfaceNames: List[MidiInterfaceIdentifier] = midi_interface_names.get().toList
  def setMidiInterfaceNames(m: List[MidiInterfaceIdentifier]): Unit = midi_interface_names_ol.setAll(m)
  def getMidiInterfaceNamesProperty: SimpleListProperty[MidiInterfaceIdentifier] = midi_interface_names

  def getMidiVstSourceNames: List[String] = midi_vst_source_names.get().toList
  def setMidiVstSourceNames(s: List[String]): Unit = midi_vst_source_names_ol.setAll(s)
  def getMidiVstSourceNamesProperty: SimpleListProperty[String] = midi_vst_source_names

  def getSelectedMidiInterface: MidiInterfaceIdentifier = selected_midi_interface.getValue
  def setSelectedMidiInterface(s: MidiInterfaceIdentifier): Unit = selected_midi_interface.setValue(s)
  def getSelectedMidiInterfaceProperty: Property[MidiInterfaceIdentifier] = selected_midi_interface

  def getSelectedMidiVst: String = selected_midi_vst.get()
  def setSelectedMidiVst(v: String): Unit = selected_midi_vst.set(v)
  def getSelectedMidiVstProperty: SimpleStringProperty = selected_midi_vst

  def initFromContext() = {
    val midiInterfaceNames = Context.midiController.getHardwareMidiDevices()
    setMidiInterfaceNames(List(null) ++ midiInterfaceNames.keys.toList)
    val vstSources = Context.midiController.getVstSources()
    setMidiVstSourceNames(List(null) ++ vstSources.map(_.getName()))
  }
}

class TrackPanel(channel: MidiChannel, model: TrackModel) extends BorderPane {
  val canvas = new KeyboardCanvas()
  @FXML var button_link_midi: Button = _
  @FXML var panel_track_main: BorderPane = _
  @FXML var combobox_midi_input: ComboBox[MidiInterfaceIdentifier] = _
  @FXML var combobox_vst_input: ComboBox[String] = _


  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/TrackPanel.fxml"))
  this.setCenter(loader.load().asInstanceOf[BorderPane])
  this.setMinWidth(100)

  this.setOnContextMenuRequested(new EventHandler[ContextMenuEvent] {
    override def handle(event: ContextMenuEvent) = {
      println(s"Context Menu")
    }
  })

  def initialize(): Unit = {
    button_link_midi.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link button pressed on Track (${channel.id})")
      }
    })

    panel_track_main.setCenter(canvas)

    combobox_midi_input.itemsProperty().bindBidirectional(model.getMidiInterfaceNamesProperty)
    combobox_midi_input.valueProperty().bindBidirectional(model.getSelectedMidiInterfaceProperty)

    combobox_midi_input.valueProperty().addListener(new ChangeListener[MidiInterfaceIdentifier]() {
      override def changed(observable: ObservableValue[_ <: MidiInterfaceIdentifier], oldValue: MidiInterfaceIdentifier, newValue: MidiInterfaceIdentifier): Unit = {
        println(s"Midi Input changed from $oldValue to $newValue")
        if(newValue != null) {
          Context.midiController.addMidiListener(newValue, new MidiListener() {
            override def midiReceived(msg: MidiMessage, timeStamp: Long): Unit = {
              if(msg.isInstanceOf[ShortMessage]) {
                channel.queueMidiMessage(msg.asInstanceOf[ShortMessage])
              } else {
                println(s"Unknown MIDI message type [$msg] [${msg.getClass.getName}]")
              }
            }
          })
        }
      }
    })

    combobox_vst_input.itemsProperty().bindBidirectional(model.getMidiVstSourceNamesProperty)
    combobox_vst_input.valueProperty().bindBidirectional(model.getSelectedMidiVstProperty)

    combobox_vst_input.valueProperty().addListener(new ChangeListener[String]() {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        println(s"Midi VST changed from $oldValue to $newValue")
        channel.setVstSource(new File(newValue))
      }
    })
  }
}
