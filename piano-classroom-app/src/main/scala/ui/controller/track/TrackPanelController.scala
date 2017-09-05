package ui.controller.track

import java.io.File
import javafx.beans.property.{Property, SimpleListProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control._
import javafx.scene.input.ContextMenuEvent
import javafx.scene.layout.BorderPane
import javax.sound.midi.{MidiMessage, ShortMessage}

import context.Context
import sound.audio.channel.MidiChannel
import sound.midi.{MidiInterfaceIdentifier, MidiListener, MidiSubscriber}
import ui.controller.component.Keyboard
import util.KeyboardNote

import scala.collection.JavaConversions._

class TrackModel(val channel: MidiChannel) {
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
    val midiInterfaceNames = Context.midiController.getHardwareMidiDevices
    setMidiInterfaceNames(List(null) ++ midiInterfaceNames.keys.toList)
    val vstSources = Context.midiController.getVstSources
    setMidiVstSourceNames(List(null) ++ vstSources.map(_.getName()))
  }
}

class TrackPanel(channel: MidiChannel, model: TrackModel) extends BorderPane {
  val canvas = new Keyboard()
  @FXML var button_link_midi: Button = _
  @FXML var button_open_vst_settings: Button = _
  @FXML var button_open_vst_source: Button = _
  @FXML var panel_track_main: BorderPane = _
  @FXML var combobox_midi_input: ComboBox[MidiInterfaceIdentifier] = _
  @FXML var combobox_vst_input: ComboBox[String] = _
  @FXML var label_track_name: Label = _

  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/TrackPanel.fxml"))
  this.setCenter(loader.load().asInstanceOf[BorderPane])
  this.setMinWidth(100)

  val _self = this

  this.setOnContextMenuRequested(new EventHandler[ContextMenuEvent] {
    override def handle(event: ContextMenuEvent) = {
      val contextMenu = new ContextMenu()

      val contextMenu_change_name = new MenuItem("Change name")
      contextMenu_change_name.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          println(s"Change name button pressed on Track (${channel.id})")
          val dialog = new TextInputDialog("walter")
          dialog.setTitle("Text Input Dialog")
          dialog.setHeaderText("Look, a Text Input Dialog")
          dialog.setContentText("Please enter your name:")

          val result = dialog.showAndWait()
          if(result.isPresent) {
            val r = result.get()
            model.setTrackName(r)
          }
        }
      })

      val contextMenu_separator = new SeparatorMenuItem()
      val contextMenu_delete = new MenuItem("Delete")
      contextMenu_delete.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          println(s"Delete name button pressed on Track (${channel.id})")
        }
      })

      contextMenu.getItems.add(contextMenu_change_name)
      contextMenu.getItems.add(contextMenu_separator)
      contextMenu.getItems.add(contextMenu_delete)

      contextMenu.show(_self, event.getScreenX, event.getScreenY)
    }
  })

  def initialize(): Unit = {
    button_link_midi.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link button pressed on Track (${channel.id})")
      }
    })

    button_open_vst_source.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        channel.vstPlugin.foreach(_.openPluginEditor(model.getTrackName))
      }
    })

    panel_track_main.setCenter(canvas)

    label_track_name.textProperty().bind(model.getTrackNameProperty)

    combobox_midi_input.itemsProperty().bindBidirectional(model.getMidiInterfaceNamesProperty)
    combobox_midi_input.valueProperty().bindBidirectional(model.getSelectedMidiInterfaceProperty)

    combobox_midi_input.valueProperty().addListener(new ChangeListener[MidiInterfaceIdentifier]() {
      override def changed(observable: ObservableValue[_ <: MidiInterfaceIdentifier], oldValue: MidiInterfaceIdentifier, newValue: MidiInterfaceIdentifier): Unit = {
        println(s"Midi Input changed from $oldValue to $newValue")
        if(newValue != null) {
          Context.midiController.unsubscribeOfAllInterfaces(channel.id)
          Context.midiController.addMidiSubscriber(newValue, MidiSubscriber(channel.id, new MidiListener() {
            override def midiReceived(msg: MidiMessage, timeStamp: Long): Unit = {
              msg match {
                case smsg: ShortMessage =>
                  channel.queueMidiMessage(msg.asInstanceOf[ShortMessage])
                  if(smsg.getCommand == ShortMessage.NOTE_ON) {
                    canvas.queueActiveNote(KeyboardNote.widthAbsoluteIndex(smsg.getData1))
                  } else if(smsg.getCommand == ShortMessage.NOTE_OFF) {
                    canvas.dequeueActiveNote(KeyboardNote.widthAbsoluteIndex(smsg.getData1))
                  }
                case _ =>
                  println(s"Unknown MIDI message type [$msg] [${msg.getClass.getName}]")
              }
            }
          }))
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