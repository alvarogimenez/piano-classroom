package ui.controller.track

import java.io.File
import java.lang.Boolean
import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control._
import javafx.scene.input.{ContextMenuEvent, MouseEvent}
import javafx.scene.layout.BorderPane
import javafx.stage.{Modality, Stage}
import javax.sound.midi.ShortMessage

import context.Context
import services.audio.channel.MidiChannel
import services.midi.{MidiInterfaceIdentifier, MidiVstSource}
import ui.controller.component.Keyboard
import ui.controller.global.ProjectSessionUpdating
import ui.controller.track.midiLink.{MidiLinkController, MidiLinkModel, _}
import ui.controller.track.pianoRange.{PianoRangeController, PianoRangeModel, _}
import util.{KeyboardNote, MidiData, MusicNote}

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TrackModel(val channel: MidiChannel) {
  final val DEFAULT_TRACK_HEIGHT = 90
  final val PIANO_ROLL_DEFAULT_HEIGHT = 160
  final val PIANO_DEFAULT_HEIGHT = 90

  var track_name = new SimpleStringProperty()
  val midi_interface_names_ol: ObservableList[MidiInterfaceIdentifier] = FXCollections.observableArrayList[MidiInterfaceIdentifier]
  val midi_interface_names: SimpleListProperty[MidiInterfaceIdentifier] = new SimpleListProperty[MidiInterfaceIdentifier](midi_interface_names_ol)
  val midi_vst_source_names_ol: ObservableList[MidiVstSource] = FXCollections.observableArrayList[MidiVstSource]
  val midi_vst_sources: SimpleListProperty[MidiVstSource] = new SimpleListProperty[MidiVstSource](midi_vst_source_names_ol)
  val selected_midi_interface: Property[MidiInterfaceIdentifier] = new SimpleObjectProperty[MidiInterfaceIdentifier]()
  val selected_midi_vst: Property[MidiVstSource] = new SimpleObjectProperty[MidiVstSource]()
  val track_height: SimpleIntegerProperty = new SimpleIntegerProperty()
  val track_piano_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val track_piano_roll_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val track_piano_start_note: SimpleObjectProperty[KeyboardNote] = new SimpleObjectProperty[KeyboardNote]()
  val track_piano_end_note: SimpleObjectProperty[KeyboardNote] = new SimpleObjectProperty[KeyboardNote]()

  def getTrackName: String = track_name.get
  def setTrackName(t: String): Unit = track_name.set(t)
  def getTrackNameProperty: SimpleStringProperty = track_name

  def getMidiInterfaceNames: List[MidiInterfaceIdentifier] = midi_interface_names.get().toList
  def setMidiInterfaceNames(m: List[MidiInterfaceIdentifier]): Unit = midi_interface_names_ol.setAll(m)
  def getMidiInterfaceNamesProperty: SimpleListProperty[MidiInterfaceIdentifier] = midi_interface_names

  def getMidiVstSources: List[MidiVstSource] = midi_vst_sources.get().toList
  def setMidiVstSources(s: List[MidiVstSource]): Unit = midi_vst_source_names_ol.setAll(s)
  def getMidiVstSourcesProperty: SimpleListProperty[MidiVstSource] = midi_vst_sources

  def getSelectedMidiInterface: MidiInterfaceIdentifier = selected_midi_interface.getValue
  def setSelectedMidiInterface(s: MidiInterfaceIdentifier): Unit = selected_midi_interface.setValue(s)
  def getSelectedMidiInterfaceProperty: Property[MidiInterfaceIdentifier] = selected_midi_interface

  def getSelectedMidiVst: MidiVstSource = selected_midi_vst.getValue
  def setSelectedMidiVst(v: MidiVstSource): Unit = selected_midi_vst.setValue(v)
  def getSelectedMidiVstProperty: Property[MidiVstSource] = selected_midi_vst

  def getTrackHeight(): Int = track_height.get
  def setTrackHeight(h: Int): Unit = track_height.set(h)
  def getTrackHeightProperty(): SimpleIntegerProperty = track_height

  def getTrackPianoEnabled(): scala.Boolean = track_piano_enabled.get
  def setTrackPianoEnabled(e: scala.Boolean): Unit = track_piano_enabled.set(e)
  def getTrackPianoEnabledProperty(): SimpleBooleanProperty = track_piano_enabled

  def getTrackPianoRollEnabled(): scala.Boolean = track_piano_roll_enabled.get
  def setTrackPianoRollEnabled(e: scala.Boolean): Unit = track_piano_roll_enabled.set(e)
  def getTrackPianoRollEnabledProperty(): SimpleBooleanProperty = track_piano_roll_enabled

  def getTrackPianoStartNote: KeyboardNote = track_piano_start_note.get
  def setTrackPianoStartNote(n: KeyboardNote): Unit = track_piano_start_note.set(n)
  def getTrackPianoStartNoteProperty: SimpleObjectProperty[KeyboardNote] = track_piano_start_note

  def getTrackPianoEndNote: KeyboardNote = track_piano_end_note.get
  def setTrackPianoEndNote(n: KeyboardNote): Unit = track_piano_end_note.set(n)
  def getTrackPianoEndNoteProperty: SimpleObjectProperty[KeyboardNote] = track_piano_end_note

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
  setTrackPianoStartNote(KeyboardNote(MusicNote.C, 2))
  setTrackPianoEndNote(KeyboardNote(MusicNote.C, 6))

  def initFromContext() = {
    val midiInterfaceNames = Context.midiService.getHardwareMidiDevices
    setMidiInterfaceNames(List(null) ++ midiInterfaceNames.keys.toList)
    val vstSources = Context.midiService.getVstSources
    setMidiVstSources(List(null) ++ vstSources)
  }

  def extractProperties: Option[Map[String, Double]] = {
    channel.getVstPlugin.flatMap { vst =>
      vst.vst.map { v =>
        val n = v.numParameters()
        (0 until n).map { pIndex =>
          pIndex.toString -> v.getParameter(pIndex).toDouble
        }.toMap
      }
    }
  }

  def applyProperties(properties: Map[String, Double]) = {
    channel.getVstPlugin.foreach { vst =>
      properties.foreach { case (property, value) =>
        vst.vst.foreach { v => v.setParameter(property.toInt, value.toFloat)}
      }
    }
  }
}

class TrackPanel(parentController: ProjectSessionUpdating, channel: MidiChannel, model: TrackModel) extends BorderPane {

  val keyboard = new Keyboard()
  @FXML var button_link_midi: Button = _
  @FXML var button_open_vst_settings: Button = _
  @FXML var button_open_vst_source: Button = _
  @FXML var button_show_piano: ToggleButton = _
  @FXML var button_show_piano_roll: ToggleButton = _
  @FXML var button_piano_range: Button = _
  @FXML var button_become_source: Button = _
  @FXML var button_switch_vst: Button = _
  @FXML var panel_track_main: BorderPane = _
  @FXML var combobox_midi_input: ComboBox[MidiInterfaceIdentifier] = _
  @FXML var combobox_vst_input: ComboBox[MidiVstSource] = _
  @FXML var label_track_name: Label = _

  channel.addMidiSubscriber(keyboard)

  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/tracks/TrackPanel.fxml"))
  val track = loader.load().asInstanceOf[BorderPane]
  track.prefHeightProperty().bind(model.getTrackHeightProperty())
  this.setCenter(track)

  val _self = this

  button_switch_vst.setOnMouseClicked(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent) = {
      val contextMenu = new ContextMenu()
      Context.trackSetModel.getTrackSet.foreach { track =>
        val item = new MenuItem(track.getTrackName)
        item.setOnAction(new EventHandler[ActionEvent] {
          override def handle(event: ActionEvent): Unit = {
            val otherVstSource = track.getSelectedMidiVst
            val otherVstProperties = track.extractProperties
            val selfVstSource = model.getSelectedMidiVst
            val selfVstProperties = model.extractProperties

            println(s"Set VSTi source in this model")
            model.setSelectedMidiVst(otherVstSource)
            println(s"Put properties to this model")
            model.channel.getVstPlugin.foreach { vstPlugin =>
              vstPlugin.vst.foreach { v =>
                Await.ready(Future {
                  while (!Try(v.isNativeComponentLoaded).getOrElse(false)) {
                    Thread.sleep(100)
                  }
                }, 30 seconds)
              }
            }
            otherVstProperties.foreach(model.applyProperties)

            println(s"Set VSTi source in the outgoing model")
            track.setSelectedMidiVst(selfVstSource)
            println(s"Put properties to the outgoing model")
            track.channel.getVstPlugin.foreach { vstPlugin =>
              vstPlugin.vst.foreach { v =>
                Await.ready(Future {
                  while (!Try(v.isNativeComponentLoaded).getOrElse(false)) {
                    Thread.sleep(100)
                  }
                }, 30 seconds)
              }
            }
            selfVstProperties.foreach(track.applyProperties)
          }
        })
        contextMenu.getItems.add(item)
      }
      contextMenu.show(_self, event.getScreenX, event.getScreenY)
    }
  })

  this.setOnContextMenuRequested(new EventHandler[ContextMenuEvent] {
    override def handle(event: ContextMenuEvent) = {
      val contextMenu = new ContextMenu()

      val contextMenu_change_name = new MenuItem("Change name")
      contextMenu_change_name.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          println(s"Change name button pressed on Track (${channel.getId})")
          val dialog = new TextInputDialog("walter")
          dialog.setTitle("Text Input Dialog")
          dialog.setHeaderText("Look, a Text Input Dialog")
          dialog.setContentText("Please enter your name:")

          val result = dialog.showAndWait()
          if(result.isPresent) {
            val r = result.get()
            model.setTrackName(r)
            parentController.updateProjectSession()
          }
        }
      })

      val contextMenu_separator = new SeparatorMenuItem()
      val contextMenu_delete = new MenuItem("Delete")
      contextMenu_delete.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent): Unit = {
          println(s"Delete name button pressed on Track (${channel.getId})")
          parentController.updateProjectSession()
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
    val msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, MidiData.SUSTAIN_DAMPER_MIDI_DATA, 0x00)
    channel.queueMidiMessage(msg)
  }

  def linkMidiDeviceModal(autoClose: Boolean = false) = {
    val dialog = new Stage()
    val loader = new FXMLLoader()
    val midiLinkModel = new MidiLinkModel()
    val controller = new MidiLinkController(dialog, midiLinkModel)

    midiLinkModel.setAutoCloseModal(autoClose)

    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/MidiDiscoverDialog.fxml"))
    loader.setController(controller)

    dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
    dialog.setResizable(false)
    dialog.setTitle("Select a MIDI source")
    dialog.initOwner(Context.primaryStage)
    dialog.initModality(Modality.APPLICATION_MODAL)
    dialog.showAndWait()

    if(midiLinkModel.getExitStatus == MIDI_LINK_MODAL_ACCEPT) {
      if(midiLinkModel.getLastMidiSource != null) {
        if(model.getMidiInterfaceNames.contains(midiLinkModel.getLastMidiSource)) {
          model.setSelectedMidiInterface(midiLinkModel.getLastMidiSource)
        }
      } else {
        model.setSelectedMidiInterface(null)
      }
      parentController.updateProjectSession()
    }
  }

  def initialize(): Unit = {
    Context.globalRenderer.addSlave(keyboard)

    button_become_source.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        Context.monitorModel.monitorWebCamModel.getTrackNoteSources.find(s => s != null && s.id == model.channel.getId).foreach { source =>
          Context.monitorModel.monitorWebCamModel.setTrackNoteSelectedSource(source)
        }
      }
    })

    button_link_midi.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link button pressed on Track (${channel.getId})")
        linkMidiDeviceModal()
      }
    })

    button_open_vst_settings.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        channel.getVstPlugin.foreach(_.openPluginEditor(model.getTrackName))
        parentController.updateProjectSession()
      }
    })

    button_piano_range.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val dialog = new Stage()
        val loader = new FXMLLoader()
        val pianoRangeModel = new PianoRangeModel()
        val controller = new PianoRangeController(dialog, pianoRangeModel)
        pianoRangeModel.setSelectedFromNote(keyboard.getStartNote.note)
        pianoRangeModel.setSelectedFromIndex(keyboard.getStartNote.index)
        pianoRangeModel.setSelectedToNote(keyboard.getEndNote.note)
        pianoRangeModel.setSelectedToIndex(keyboard.getEndNote.index)

        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/PianoRangeDialog.fxml"))
        loader.setController(controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Select a Piano Range")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if(pianoRangeModel.getExitStatus == PIANO_RANGE_MODAL_ACCEPT) {
          model.setTrackPianoStartNote(KeyboardNote(pianoRangeModel.getSelectedFromNote, pianoRangeModel.getSelectedFromIndex))
          model.setTrackPianoEndNote(KeyboardNote(pianoRangeModel.getSelectedToNote, pianoRangeModel.getSelectedToIndex))
          parentController.updateProjectSession()
        }
      }
    })


    keyboard.getPianoEnabledProperty.bindBidirectional(model.getTrackPianoEnabledProperty())
    keyboard.getPianoRollEnabledProperty.bindBidirectional(model.getTrackPianoRollEnabledProperty())
    keyboard.getStartNoteProperty.bind(model.getTrackPianoStartNoteProperty)
    keyboard.getEndNoteProperty.bind(model.getTrackPianoEndNoteProperty)

    model.getTrackPianoEnabledProperty().addListener(new InvalidationListener {
      override def invalidated(observable: Observable) = parentController.updateProjectSession()
    })
    model.getTrackPianoRollEnabledProperty().addListener(new InvalidationListener {
      override def invalidated(observable: Observable) = parentController.updateProjectSession()
    })

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
          channel.stopListening()
          channel.listen(newValue)
        }
        parentController.updateProjectSession()
      }
    })

    combobox_vst_input.itemsProperty().bindBidirectional(model.getMidiVstSourcesProperty)
    combobox_vst_input.valueProperty().bindBidirectional(model.getSelectedMidiVstProperty)

    model.getSelectedMidiVstProperty.addListener(new ChangeListener[MidiVstSource]() {
      override def changed(observable: ObservableValue[_ <: MidiVstSource], oldValue: MidiVstSource, newValue: MidiVstSource): Unit = {
        println(s"Midi VST changed from $oldValue to $newValue")
        if(newValue != null) {
          channel.close()
          channel.setVstSource(new File(newValue.path))
        } else {
          channel.close()
        }
        parentController.updateProjectSession()
      }
    })
  }
}
