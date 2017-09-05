package ui.controller

import java.util.UUID
import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.{Modality, Stage}

import context.Context
import sound.audio.channel.MidiChannel
import sound.audio.mixer.ChannelMix
import ui.controller.mixer.MixerController
import ui.controller.settings.SettingsController
import ui.controller.track._


class MainStageController
  extends MenuBarController
    with MixerController
    with TrackSetController {

  @FXML var fileClose: MenuItem = _
  @FXML var fileTest: MenuItem = _
  @FXML var editSettings: MenuItem = _


  def initialize(): Unit = {
    initializeMenuController()
    initializeMixerController()
    initializeTrackSetController()

    fileClose.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        Platform.exit()
      }
    })

    fileTest.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val midiChannel = new MidiChannel(UUID.randomUUID().toString)
        val model = new TrackModel(midiChannel)
        model.setTrackName(s"Track ${midiChannel.id.take(4)}")
        model.initFromContext()
        Context.channelService.addChannel(midiChannel)
        Context.trackSetModel.addTrack(model)
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
