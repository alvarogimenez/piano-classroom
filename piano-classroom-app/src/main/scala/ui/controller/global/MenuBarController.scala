package ui.controller.global

import java.io.File
import java.util.UUID
import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.stage.{FileChooser, Modality, Stage}

import context.Context
import io.contracts.{GlobalConfiguration, GlobalIoConfiguration}
import sound.audio.channel.MidiChannel
import ui.controller.MainStageController
import ui.controller.settings.SettingsController
import ui.controller.track.TrackModel

trait MenuBarController {
  @FXML var menu_file_open: MenuItem = _
  @FXML var menu_file_close: MenuItem = _
  @FXML var menu_edit_settings: MenuItem = _
  @FXML var menu_edit_add_midi_channel: MenuItem = _
  @FXML var menu_edit_add_audio_channel: MenuItem = _

  def initializeMenuController(mainController: MainStageController) = {
    menu_file_close.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        Platform.exit()
      }
    })

    menu_file_open.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val fc = new FileChooser()
        fc.setTitle("Select a Project File")
        fc.setInitialDirectory(new File("."))
        fc.getExtensionFilters.addAll(
          new FileChooser.ExtensionFilter("Project Files", "*.json"),
          new FileChooser.ExtensionFilter("All Files", "*.*")
        )
        val file = fc.showOpenDialog(Context.primaryStage)
        if(file != null) {
          Context.projectSession.set(context.readProjectSession(Some(file.getAbsolutePath)))
          context.loadProjectSession(mainController)

          val ioConfiguration =
            Context
              .applicationSession
              .get()
              .`global`
              .flatMap(_.`io`)
              .getOrElse(GlobalIoConfiguration())
              .copy(`last-opened-file` = Some(file.getAbsolutePath))

          context.writeApplicationSessionSettings(
            Context.applicationSession.get().copy(
              `global` =
                Some(
                  Context.applicationSession.get().`global`
                    .getOrElse(GlobalConfiguration())
                    .copy(`io` = Some(ioConfiguration))
                )
            )
          )
        }
      }
    })

    menu_edit_settings.setOnAction(new EventHandler[ActionEvent] {
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

    menu_edit_add_midi_channel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val midiChannel = new MidiChannel(UUID.randomUUID().toString)
        val model = new TrackModel(midiChannel)
        model.setTrackName(s"Track ${midiChannel.id.take(4)}")
        model.initFromContext()
        Context.channelService.addChannel(midiChannel)
        Context.trackSetModel.addTrack(model)
      }
    })
  }
}
