package ui.controller.global.profileSave

import java.lang.Boolean
import java.util.concurrent.Callable
import javafx.beans.binding.Bindings
import javafx.beans.property.{SimpleListProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.collections.{FXCollections, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.stage.Stage

import ui.controller.global.profileSave.ProfileSaveAction.ProfileSaveAction

import scala.collection.JavaConversions._

class ProfileSaveModel {
  var exit_status: Int = _
  val profile_names_ol: ObservableList[String] = FXCollections.observableArrayList[String]
  val profile_names: SimpleListProperty[String] = new SimpleListProperty[String](profile_names_ol)
  val result_action: SimpleObjectProperty[ProfileSaveAction] = new SimpleObjectProperty[ProfileSaveAction]()
  val result_name: SimpleStringProperty = new SimpleStringProperty()

  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  def getProfileNames: List[String] = profile_names.get().toList
  def setProfileNames(m: List[String]): Unit = profile_names_ol.setAll(m)
  def getProfileNamesProperty: SimpleListProperty[String] = profile_names

  def getResultAction: ProfileSaveAction = result_action.get()
  def setResultAction(a: ProfileSaveAction): Unit = result_action.set(a)
  def getResultActionProperty: SimpleObjectProperty[ProfileSaveAction] = result_action

  def getResultName: String = result_name.get()
  def setResultName(n: String): Unit = result_name.set(n)
  def getResultNameProperty: SimpleStringProperty = result_name
}

class ProfileSaveController(dialog: Stage, model: ProfileSaveModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var radiobutton_override: RadioButton = _
  @FXML var radiobutton_new: RadioButton = _
  @FXML var profile_creation_group: ToggleGroup = _
  @FXML var listview_override_names: ListView[String] = _
  @FXML var textfield_name: TextField = _

  def initialize() : Unit = {
    radiobutton_override.setUserData(ProfileSaveAction.OVERRIDE)
    radiobutton_new.setUserData(ProfileSaveAction.NEW)
    model.getResultActionProperty.bind(Bindings.createObjectBinding[ProfileSaveAction](
      new Callable[ProfileSaveAction] {
        override def call(): ProfileSaveAction = {
          profile_creation_group.getSelectedToggle.getUserData.asInstanceOf[ProfileSaveAction]
        }
      },
      profile_creation_group.selectedToggleProperty()))
    model.getResultNameProperty.bind(Bindings.createStringBinding(
      new Callable[String] {
        override def call(): String = {
          if(radiobutton_override.isSelected && !listview_override_names.getSelectionModel.getSelectedItems.isEmpty) {
            listview_override_names.getSelectionModel.getSelectedItems.head
          } else if(radiobutton_new.isSelected) {
            textfield_name.getText.trim
          } else {
            null
          }
        }
      },
      listview_override_names.getSelectionModel.getSelectedItems,
      textfield_name.textProperty(),
      radiobutton_new.selectedProperty(),
      radiobutton_override.selectedProperty()))

    textfield_name.disableProperty().bind(radiobutton_new.selectedProperty().not())
    listview_override_names.disableProperty().bind(radiobutton_override.selectedProperty().not())
    radiobutton_override.disableProperty().bind(Bindings.isEmpty(model.profile_names_ol))

    listview_override_names.itemsProperty().bind(model.getProfileNamesProperty)

    button_accept.disableProperty().bind(Bindings.createBooleanBinding(
      new Callable[Boolean] {
        override def call(): Boolean = {
          if(radiobutton_override.isSelected) {
            listview_override_names.getSelectionModel.getSelectedItems.isEmpty
          } else if(radiobutton_new.isSelected) {
            textfield_name.getText.trim.isEmpty
          } else {
            true
          }
        }
      },
      listview_override_names.getSelectionModel.getSelectedItems,
      textfield_name.textProperty(),
      radiobutton_new.selectedProperty(),
      radiobutton_override.selectedProperty()))

    button_cancel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(PROFILE_SAVE_MODAL_CANCEL)
        dialog.close()
      }
    })

    button_accept.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(PROFILE_SAVE_MODAL_ACCEPT)
        dialog.close()
      }
    })
  }

}
