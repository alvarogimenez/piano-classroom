package ui.controller.track.pianoRange

import javafx.beans.{InvalidationListener, Observable}
import javafx.beans.property.{SimpleIntegerProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, ComboBox}
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

import ui.controller.component.Keyboard
import util.{KeyboardNote, MusicNote}
import util.MusicNote.MusicNote

import scala.collection.JavaConversions._

class PianoRangeModel {
  val available_notes_ol: ObservableList[MusicNote] = FXCollections.observableArrayList[MusicNote]
  val available_notes: SimpleListProperty[MusicNote] = new SimpleListProperty[MusicNote](available_notes_ol)
  val available_note_indexes_ol: ObservableList[Int] = FXCollections.observableArrayList[Int]
  val available_note_indexes: SimpleListProperty[Int] = new SimpleListProperty[Int](available_note_indexes_ol)
  val selected_from_note: SimpleObjectProperty[MusicNote] = new SimpleObjectProperty[MusicNote]()
  val selected_from_index: SimpleObjectProperty[Int] = new SimpleObjectProperty[Int]()
  val selected_to_note: SimpleObjectProperty[MusicNote] = new SimpleObjectProperty[MusicNote]()
  val selected_to_index: SimpleObjectProperty[Int] = new SimpleObjectProperty[Int]()
  var exit_status: Int = _

  def getAvailableNotes: List[MusicNote] = available_notes.get().toList
  def setAvailableNotes(x: List[MusicNote]):Unit = available_notes.setAll(x)
  def getAvailableNotesProperty: SimpleListProperty[MusicNote] = available_notes

  def getAvailableNoteIndexes: List[Int] = available_note_indexes.get().toList
  def setAvailableNoteIndexes(x: List[Int]):Unit = available_note_indexes.setAll(x)
  def getAvailableNoteIndexesProperty: SimpleListProperty[Int] = available_note_indexes

  def getSelectedFromNote: MusicNote = selected_from_note.get
  def setSelectedFromNote(n: MusicNote): Unit = selected_from_note.set(n)
  def getSelectedFromNoteProperty: SimpleObjectProperty[MusicNote] = selected_from_note

  def getSelectedFromIndex: Int = selected_from_index.get
  def setSelectedFromIndex(i: Int) = selected_from_index.set(i)
  def getSelectedFromIndexProperty: SimpleObjectProperty[Int] = selected_from_index

  def getSelectedToNote: MusicNote = selected_to_note.get
  def setSelectedToNote(n: MusicNote): Unit = selected_to_note.set(n)
  def getSelectedToNoteProperty: SimpleObjectProperty[MusicNote] = selected_to_note

  def getSelectedToIndex: Int = selected_to_index.get
  def setSelectedToIndex(i: Int) = selected_to_index.set(i)
  def getSelectedToIndexProperty: SimpleObjectProperty[Int] = selected_to_index

  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  setAvailableNotes(MusicNote.values.toList)
  setAvailableNoteIndexes((0 to 8).toList)
}

class PianoRangeController(dialog: Stage, model: PianoRangeModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var bpane_keyboard: BorderPane = _
  @FXML var combo_from_note: ComboBox[MusicNote] = _
  @FXML var combo_from_index: ComboBox[Int] = _
  @FXML var combo_to_note: ComboBox[MusicNote] = _
  @FXML var combo_to_index: ComboBox[Int] = _

  val canvas = new Keyboard()
  canvas.setPianoEnabled(true)
  canvas.setPianoRollEnabled(false)

  def initialize() : Unit = {
    bpane_keyboard.setCenter(canvas)

    combo_from_note.itemsProperty().bind(model.getAvailableNotesProperty)
    combo_from_index.itemsProperty().bind(model.getAvailableNoteIndexesProperty)
    combo_to_note.itemsProperty().bind(model.getAvailableNotesProperty)
    combo_to_index.itemsProperty().bind(model.getAvailableNoteIndexesProperty)

    combo_from_note.valueProperty().bindBidirectional(model.getSelectedFromNoteProperty)
    combo_from_index.valueProperty().bindBidirectional(model.getSelectedFromIndexProperty)

    combo_to_note.valueProperty().bindBidirectional(model.getSelectedToNoteProperty)
    combo_to_index.valueProperty().bindBidirectional(model.getSelectedToIndexProperty)

    val noteChangeListener = new ChangeListener[Any] {
      override def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
        setCanvasNoteFromModel()
      }
    }

    combo_from_note.valueProperty().addListener(noteChangeListener)
    combo_from_index.valueProperty().addListener(noteChangeListener)
    combo_to_note.valueProperty().addListener(noteChangeListener)
    combo_to_index.valueProperty().addListener(noteChangeListener)

    button_cancel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(PIANO_RANGE_MODAL_CANCEL)
        dialog.close()
      }
    })

    button_accept.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(PIANO_RANGE_MODAL_ACCEPT)
        dialog.close()
      }
    })

    setCanvasNoteFromModel()
  }

  def setCanvasNoteFromModel() = {
    canvas.setStartNote(KeyboardNote(model.getSelectedFromNote, model.getSelectedFromIndex))
    canvas.setEndNote(KeyboardNote(model.getSelectedToNote, model.getSelectedToIndex))
  }
}
