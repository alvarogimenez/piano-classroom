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
import ui.controller.monitor.MonitorController
import ui.controller.settings.SettingsController
import ui.controller.track._


class MainStageController
  extends MenuBarController
    with MixerController
    with TrackSetController
    with MonitorController {

  def initialize(): Unit = {
    initializeMenuController()
    initializeMixerController()
    initializeTrackSetController()
    initializeMonitorController()
  }
}
