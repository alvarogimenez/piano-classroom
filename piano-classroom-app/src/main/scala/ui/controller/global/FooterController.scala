package ui.controller.global

import java.io.File
import java.lang.Boolean
import java.util.concurrent.Callable
import javafx.beans.binding.Bindings
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, ProgressIndicator, ToggleButton}
import javafx.scene.layout.BorderPane
import javafx.stage.{Modality, Stage}

import context.Context
import ui.controller.track.{TrackPanel, TrackSetController}
import ui.controller.{MainStageController, global}

import scala.collection.JavaConversions._

trait FooterController { _: TrackSetController =>
  @FXML var button_test: ToggleButton = _
  @FXML var button_refresh_rendering: Button = _
  @FXML var button_clear_all: Button = _
  @FXML var button_panic: Button = _
  @FXML var button_link_all: Button = _
  @FXML var button_reopen_devices: Button = _
  @FXML var label_open_project: Label = _
  @FXML var progress_saving: ProgressIndicator = _

  def initializeFooterController(mainController: MainStageController) = {
    progress_saving.visibleProperty().bind(Context.projectSessionSaving)
    label_open_project.textProperty().bind(Bindings.createStringBinding(new Callable[String] {
      override def call() = {
        Context
          .applicationSession
          .get()
          .`global`
          .flatMap(_.`io`.flatMap(_.`last-opened-file`))
          .map(f => new File(f).getName)
          .getOrElse("No file opened")
      }
    }, Context.applicationSession))

    button_test.selectedProperty().addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = {
        if (newValue) {
          import global.deviceTestConfiguration._

          val dialog = new Stage()
          val loader = new FXMLLoader()
          val model = new DeviceTestConfigurationModel()
          val controller = new DeviceTestConfigurationController(dialog, model)

          loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/DeviceTestConfigurationDialog.fxml"))
          loader.setController(controller)

          dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
          dialog.setResizable(false)
          dialog.setTitle("Test mode configuration")
          dialog.initOwner(Context.primaryStage)
          dialog.initModality(Modality.APPLICATION_MODAL)
          dialog.showAndWait()

          if (model.getExitStatus == DEVICE_TEST_CONFIGURATION_MODAL_ACCEPT) {
            println(s"-------------------------------")
            println(s"START TEST")
            println(s"-------------------------------")
            println(model.getSelectedMidiInterfaces.getSelectedItems.toList)
            println(model.getSustainPedalActive)
            println(s"-------------------------------")
            Context.midiService.startTestTask(
              sources = model.getSelectedMidiInterfaces.getSelectedItems.toList,
              sustainOn = model.getSustainPedalActive
            )
          }
        } else {
          println(s"-------------------------------")
          println(s"END TEST")
          println(s"-------------------------------")
          Context.midiService.stopTestTask()
        }
      }
    })

    button_link_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Link all MIDI Devices...")
        linkAllMidiDevices()
      }
    })

    button_reopen_devices.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Reopen all devices...")
        Context.midiService.detach()
        Context.midiService.attach()
      }
    })

    button_refresh_rendering.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Refresh rendering...")
        Context.globalRenderer.stopThread()
        Thread.sleep(500)
        Context.globalRenderer.startThread()
      }
    })

    button_clear_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Full data clear!")
        tracks
          .getChildren
          .foreach {
            case trackPanel: TrackPanel =>
              trackPanel.clear()
            case _ =>
          }
      }
    })

    button_panic.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"MIDI PANIC !!!")
        tracks
          .getChildren
          .foreach {
            case trackPanel: TrackPanel =>
              trackPanel.clear()
              trackPanel.panic()
            case _ =>
          }
      }
    })
  }
}
