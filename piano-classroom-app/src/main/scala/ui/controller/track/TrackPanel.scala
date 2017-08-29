package ui.controller.track

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{Button, ComboBox, Tooltip}
import javafx.scene.layout.BorderPane

import context.Context
import sound.midi.MidiInterfaceIdentifier

case class TrackPanelInitialSettings(
  midiIn: MidiInterfaceIdentifier,
  vstSource: String
)

class TrackPanel(index: Int, settings: Option[TrackPanelInitialSettings] = None) extends BorderPane {
  val canvas = new KeyboardCanvas()
  @FXML var button_link_midi: Button = _
  @FXML var panel_track_main: BorderPane = _
  @FXML var combobox_midi_input: ComboBox[MidiInterfaceIdentifier] = _


  val loader = new FXMLLoader()
  loader.setController(this)
  loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/TrackPanel.fxml"))
  this.setCenter(loader.load().asInstanceOf[BorderPane])
  this.setMinWidth(100)

  def initialize(): Unit = {
    button_link_midi.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link button pressed on Track ($index)")
      }
    })

    panel_track_main.setCenter(canvas)

    val hardwareMidiDeviceNames = Context.midiController.getHardwareMidiDevices().keys.toSeq
    val combobox_midi_input_options = FXCollections.observableArrayList[MidiInterfaceIdentifier](hardwareMidiDeviceNames:_*)
    combobox_midi_input.setItems(combobox_midi_input_options)
    combobox_midi_input.valueProperty().addListener(new ChangeListener[MidiInterfaceIdentifier]() {
      override def changed(observable: ObservableValue[_ <: MidiInterfaceIdentifier], oldValue: MidiInterfaceIdentifier, newValue: MidiInterfaceIdentifier): Unit = {
        println(s"Midi Input changed from $oldValue to $newValue")
        combobox_midi_input.setTooltip(new Tooltip(newValue.name))
      }
    })
    settings match {
      case Some(s) =>
        if(hardwareMidiDeviceNames.contains(s.midiIn))
          combobox_midi_input.getSelectionModel.select(s.midiIn)
        else
          combobox_midi_input.getSelectionModel.selectFirst()
      case None =>
    }
  }
}
