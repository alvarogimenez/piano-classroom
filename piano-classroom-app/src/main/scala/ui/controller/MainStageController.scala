package ui.controller

import javafx.application.{Application, Platform}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.{Modality, Stage}

import context.Context
import sound.midi.MidiInterfaceIdentifier
import ui.controller.settings.SettingsController
import ui.controller.track.{TrackPanel, TrackPanelInitialSettings}


class MainStageController extends MenuBarController {
  @FXML var tracks: VBox = _
  @FXML var fileClose: MenuItem = _
  @FXML var fileTest: MenuItem = _
  @FXML var editSettings: MenuItem = _

  def initialize(): Unit = {
    fileClose.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        Platform.exit()
      }
    })
    fileTest.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        tracks.getChildren.add(new TrackPanel(0, Some(TrackPanelInitialSettings(midiIn = MidiInterfaceIdentifier("[LoopBe Internal MIDI]:1"), vstSource = ""))))
        tracks.getChildren.add(new TrackPanel(1))
      }
    })
    editSettings.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val dialog = new Stage()
        val loader = new FXMLLoader()
        loader.setController(this)
        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/Settings.fxml"))
        loader.setController(new SettingsController(dialog))

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Settings")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()
      }
    })

  }
}
