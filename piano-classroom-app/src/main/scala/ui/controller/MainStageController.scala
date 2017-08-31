package ui.controller

import java.util.UUID
import javafx.application.{Application, Platform}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.{Modality, Stage}

import context.Context
import sound.audio.channel.MidiChannel
import sound.audio.mixer.ChannelMix
import sound.midi.MidiInterfaceIdentifier
import ui.controller.settings.SettingsController
import ui.controller.track.{TrackModel, TrackPanel, TrackPanelInitialSettings}


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
        val midiChannel = new MidiChannel(UUID.randomUUID().toString)
        val trackModel = new TrackModel()
        trackModel.initFromContext()

        Context.channelController.addChannel(midiChannel)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 0)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 1)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 2)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 3)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 4)
        Context.mixerController.setChannelInOutput(ChannelMix(midiChannel.id, 1f), 5)
        tracks.getChildren.add(new TrackPanel(midiChannel, trackModel))
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
