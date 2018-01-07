package ui.controller.global.deviceTestConfiguration

import javafx.beans.property.{SimpleBooleanProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.stage.Stage

import context.Context
import sound.midi.MidiInterfaceIdentifier

import scala.collection.JavaConversions._

class DeviceTestConfigurationModel {
  var exit_status: Int = _
  val midi_interface_names_ol: ObservableList[MidiInterfaceIdentifier] = FXCollections.observableArrayList[MidiInterfaceIdentifier]
  val midi_interface_names: SimpleListProperty[MidiInterfaceIdentifier] = new SimpleListProperty[MidiInterfaceIdentifier](midi_interface_names_ol)
  val selected_midi_interfaces: SimpleObjectProperty[MultipleSelectionModel[MidiInterfaceIdentifier]] = new SimpleObjectProperty[MultipleSelectionModel[MidiInterfaceIdentifier]]()
  val sustain_pedal_active: SimpleBooleanProperty = new SimpleBooleanProperty()

  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  def getMidiInterfaceNames: List[MidiInterfaceIdentifier] = midi_interface_names.get().toList
  def setMidiInterfaceNames(m: List[MidiInterfaceIdentifier]): Unit = midi_interface_names_ol.setAll(m)
  def getMidiInterfaceNamesProperty: SimpleListProperty[MidiInterfaceIdentifier] = midi_interface_names

  def getSustainPedalActive: Boolean = sustain_pedal_active.get()
  def setSustainPedalActive(a: Boolean): Unit = sustain_pedal_active.set(a)
  def getSustainPedalActiveProperty: SimpleBooleanProperty = sustain_pedal_active

  def getSelectedMidiInterfaces: MultipleSelectionModel[MidiInterfaceIdentifier] = selected_midi_interfaces.get()
  def setSelectedMidiInterfaces(m: MultipleSelectionModel[MidiInterfaceIdentifier]) = selected_midi_interfaces.set(m)
  def getSelectedMidiInterfacesProperty: SimpleObjectProperty[MultipleSelectionModel[MidiInterfaceIdentifier]] = selected_midi_interfaces

  setMidiInterfaceNames(Context.midiService.getHardwareMidiDevices.keys.toList)
}

class DeviceTestConfigurationController(dialog: Stage, model: DeviceTestConfigurationModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var listview_devices: ListView[MidiInterfaceIdentifier] = _
  @FXML var checkbox_sustain_pedal: CheckBox = _
  @FXML var button_clear: Button = _
  @FXML var button_select_all: Button = _

  def initialize() : Unit = {
    listview_devices.itemsProperty().bind(model.getMidiInterfaceNamesProperty)
    listview_devices.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
    model.getSelectedMidiInterfacesProperty.bind(listview_devices.selectionModelProperty())

    checkbox_sustain_pedal.selectedProperty().bindBidirectional(model.getSustainPedalActiveProperty)

    button_clear.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        listview_devices.getSelectionModel.clearSelection()
      }
    })

    button_select_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        listview_devices.getSelectionModel.selectAll()
      }
    })

    button_cancel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(DEVICE_TEST_CONFIGURATION_MODAL_CANCEL)
        dialog.close()
      }
    })

    button_accept.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(DEVICE_TEST_CONFIGURATION_MODAL_ACCEPT)
        dialog.close()
      }
    })
  }

}
