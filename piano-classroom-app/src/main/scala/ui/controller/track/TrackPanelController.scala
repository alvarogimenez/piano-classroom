package ui.controller.track

import java.io.File
import java.lang.Boolean
import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control._
import javafx.scene.input.ContextMenuEvent
import javafx.scene.layout.BorderPane
import javafx.stage.{Modality, Stage}
import javax.sound.midi.{MidiMessage, ShortMessage}

import context.Context
import sound.audio.channel.MidiChannel
import sound.midi.{MidiInterfaceIdentifier, MidiListener, MidiSubscriber}
import ui.controller.component.Keyboard
import ui.controller.track.pianoRange.{PianoRangeController, PianoRangeModel}
import util.{KeyboardNote, MusicNote}
import pianoRange._

import scala.collection.JavaConversions._

class TrackModel(val channel: MidiChannel) {
  final val DEFAULT_TRACK_HEIGHT = 90
  final val PIANO_ROLL_DEFAULT_HEIGHT = 160
  final val PIANO_DEFAULT_HEIGHT = 90

  var track_subscribers: List[TrackSubscriber] = List.empty

  var track_name = new SimpleStringProperty()
  val midi_interface_names_ol: ObservableList[MidiInterfaceIdentifier] = FXCollections.observableArrayList[MidiInterfaceIdentifier]
  val midi_interface_names: SimpleListProperty[MidiInterfaceIdentifier] = new SimpleListProperty[MidiInterfaceIdentifier](midi_interface_names_ol)
  val midi_vst_source_names_ol: ObservableList[String] = FXCollections.observableArrayList[String]
  val midi_vst_source_names: SimpleListProperty[String] = new SimpleListProperty[String](midi_vst_source_names_ol)
  val selected_midi_interface: Property[MidiInterfaceIdentifier] = new SimpleObjectProperty[MidiInterfaceIdentifier]()
  val selected_midi_vst: SimpleStringProperty = new SimpleStringProperty()
  val track_height: SimpleIntegerProperty = new SimpleIntegerProperty()
  val track_piano_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val track_piano_roll_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()

  def addTrackSubscriber(s: TrackSubscriber): Unit = track_subscribers = track_subscribers.+:(s)
  def removeTrackSubscriber(s: TrackSubscriber): Unit = track_subscribers = track_subscribers.filterNot(_ == s)
  def clearTrackSubscribers(): Unit = track_subscribers = List.empty

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
  
  def getTrackHeight(): Int = track_height.get
  def setTrackHeight(h: Int): Unit = track_height.set(h)
  def getTrackHeightProperty(): SimpleIntegerProperty = track_height

  def getTrackPianoEnabled(): scala.Boolean = track_piano_enabled.get
  def setTrackPianoEnabled(e: scala.Boolean): Unit = track_piano_enabled.set(e)
  def getTrackPianoEnabledProperty(): SimpleBooleanProperty = track_piano_enabled

  def getTrackPianoRollEnabled(): scala.Boolean = track_piano_roll_enabled.get
  def setTrackPianoRollEnabled(e: scala.Boolean): Unit = track_piano_roll_enabled.set(e)
  def getTrackPianoRollEnabledProperty(): SimpleBooleanProperty = track_piano_roll_enabled

  val heightListener = new ChangeListener[Boolean] {
    override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = {
      val sumHeight =
        (if (getTrackPianoEnabled()) PIANO_DEFAULT_HEIGHT else 0) +
      (if (getTrackPianoRollEnabled()) PIANO_ROLL_DEFAULT_HEIGHT else 0)
      setTrackHeight(Math.max(DEFAULT_TRACK_HEIGHT, sumHeight))
    }
  }

  track_piano_enabled.addListener(heightListener)
  track_piano_roll_enabled.addListener(heightListener)

  setTrackHeight(300)
  setTrackPianoEnabled(true)
  setTrackPianoRollEnabled(true)
  
  def initFromContext() = {
    val midiInterfaceNames = Context.midiService.getHardwareMidiDevices
    setMidiInterfaceNames(List(null) ++ midiInterfaceNames.keys.toList)
    val vstSources = Context.midiService.getVstSources
    setMidiVstSourceNames(List(null) ++ vstSources.map(_.getName()))
  }
}

class TrackPanel(channel: MidiChannel, model: TrackModel) extends BorderPane {
  final val SUSTAIN_DAMPER_MIDI_DATA = 0x40

  val keyboard = new Keyboard()
  @FXML var button_link_midi: Button = _
  @FXML var button_open_vst_settings: Button = _
  @FXML var button_open_vst_source: Button = _
  @FXML var button_show_piano: ToggleButton = _
  @FXML var button_show_piano_roll: ToggleButton = _
  @FXML var button_piano_range: Button = _
  @FXML var panel_track_main: BorderPane = _
  @FXML var combobox_midi_input: ComboBox[MidiInterfaceIdentifier] = _
  @FXML var combobox_vst_input: ComboBox[String] = _
  @FXML var label_track_name: Label = _

  model.addTrackSubscriber(keyboard)

  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/TrackPanel.fxml"))
  val track = loader.load().asInstanceOf[BorderPane]
  track.prefHeightProperty().bind(model.getTrackHeightProperty())
  this.setCenter(track)

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

  def clear(): Unit = {
    keyboard.clear()
  }

  def panic(): Unit = {
    (0 to 127)
      .foreach { note =>
        val msg = new ShortMessage(ShortMessage.NOTE_OFF, note, 0x00)
        channel.queueMidiMessage(msg)
      }
    val msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, SUSTAIN_DAMPER_MIDI_DATA, 0x00)
    channel.queueMidiMessage(msg)
  }

  def initialize(): Unit = {
    button_link_midi.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link button pressed on Track (${channel.id})")
      }
    })

    button_open_vst_settings.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        channel.vstPlugin.foreach(_.openPluginEditor(model.getTrackName))
      }
    })

    button_piano_range.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val dialog = new Stage()
        val loader = new FXMLLoader()
        val model = new PianoRangeModel()
        val controller = new PianoRangeController(dialog, model)
        model.setSelectedFromNote(keyboard.getStartNote.note)
        model.setSelectedFromIndex(keyboard.getStartNote.index)
        model.setSelectedToNote(keyboard.getEndNote.note)
        model.setSelectedToIndex(keyboard.getEndNote.index)

        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/PianoRangeDialog.fxml"))
        loader.setController(controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Select a Piano Range")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if(model.getExitStatus == PIANO_RANGE_MODAL_ACCEPT) {
          keyboard.setStartNote(KeyboardNote(model.getSelectedFromNote, model.getSelectedFromIndex))
          keyboard.setEndNote(KeyboardNote(model.getSelectedToNote, model.getSelectedToIndex))
        }
      }
    })

    keyboard.getPianoEnabledProperty.bindBidirectional(model.getTrackPianoEnabledProperty())
    keyboard.getPianoRollEnabledProperty.bindBidirectional(model.getTrackPianoRollEnabledProperty())
    keyboard.setStartNote(KeyboardNote(MusicNote.C, 2))
    keyboard.setEndNote(KeyboardNote(MusicNote.C, 6))

    button_show_piano.selectedProperty().bindBidirectional(model.getTrackPianoEnabledProperty())
    button_show_piano_roll.selectedProperty().bindBidirectional(model.getTrackPianoRollEnabledProperty())

    panel_track_main.setCenter(keyboard)

    label_track_name.textProperty().bind(model.getTrackNameProperty)

    combobox_midi_input.itemsProperty().bindBidirectional(model.getMidiInterfaceNamesProperty)
    combobox_midi_input.valueProperty().bindBidirectional(model.getSelectedMidiInterfaceProperty)

    model.getSelectedMidiInterfaceProperty.addListener(new ChangeListener[MidiInterfaceIdentifier]() {
      override def changed(observable: ObservableValue[_ <: MidiInterfaceIdentifier], oldValue: MidiInterfaceIdentifier, newValue: MidiInterfaceIdentifier): Unit = {
        println(s"Midi Input changed from $oldValue to $newValue")
        if(newValue != null) {
          Context.midiService.unsubscribeOfAllInterfaces(channel.id)
          Context.midiService.addMidiSubscriber(newValue, MidiSubscriber(channel.id, new MidiListener() {
            override def midiReceived(msg: MidiMessage, timeStamp: Long): Unit = {
              msg match {
                case smsg: ShortMessage =>
                  channel.queueMidiMessage(msg.asInstanceOf[ShortMessage])
                  if(smsg.getCommand == ShortMessage.NOTE_ON && smsg.getData2 > 0) {
                    model.track_subscribers.foreach(_.noteOn(KeyboardNote.widthAbsoluteIndex(smsg.getData1 - 12)))
                  } else if(smsg.getCommand == ShortMessage.NOTE_OFF || (smsg.getCommand == ShortMessage.NOTE_ON && smsg.getData2 == 0)) {
                    model.track_subscribers.foreach(_.noteOff(KeyboardNote.widthAbsoluteIndex(smsg.getData1 - 12)))
                  } else if(smsg.getCommand == ShortMessage.CONTROL_CHANGE && smsg.getData1 == SUSTAIN_DAMPER_MIDI_DATA) {
                    if(smsg.getData2 < 64) {
                      model.track_subscribers.foreach(_.sustainOff())
                    } else {
                      model.track_subscribers.foreach(_.sustainOn())
                    }
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

    model.getSelectedMidiVstProperty.addListener(new ChangeListener[String]() {
      override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
        println(s"Midi VST changed from $oldValue to $newValue")
        channel.setVstSource(new File(newValue))
      }
    })
  }
}
