package ui.controller

import java.io.{File, FileNotFoundException, PrintWriter}
import java.util.UUID
import javafx.application.Platform
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.layout.BorderPane
import javafx.stage.{FileChooser, Modality, Stage}

import context.Context
import io.contracts.{SaveContract, SessionContract, initializeEmptySesionContract}
import io.{fromJson, toJson}
import sound.audio.channel.MidiChannel
import ui.controller.mixer.{BusChannelModel, BusMixModel}
import ui.controller.settings.SettingsController
import ui.controller.track.TrackModel

import scala.io.Source
import scala.util.Try

trait MenuBarController {
  @FXML var menu_file_open: MenuItem = _
  @FXML var menu_file_close: MenuItem = _
  @FXML var menu_edit_settings: MenuItem = _
  @FXML var menu_edit_add_midi_channel: MenuItem = _
  @FXML var menu_edit_add_audio_channel: MenuItem = _

  def initializeMenuController() = {
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
          Try(Source.fromFile(file).mkString)
            .map(fromJson[SaveContract])
            .recoverWith {
              case e: FileNotFoundException =>
                println(e)
                throw new Exception(s"File not found '${file.getName}': '${e.getMessage}'")
              case e: Exception =>
                println(e)
                throw new Exception(s"Error while reading '${file.getName}': '${e.getMessage}'")
            }
            .foreach { saveContract =>
              load(saveContract)
            }
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

  def load(save: SaveContract): Unit = {
    save
      .`save-state`
      .`tracks`
      .`channel-info`
      .foreach { channelInfo =>
        Context.channelService.removeChannels()
        Context.trackSetModel.clear()

        val midiChannel = new MidiChannel(channelInfo.`id`)
        val model = new TrackModel(midiChannel)

        Context.channelService.addChannel(midiChannel)
        Context.trackSetModel.addTrack(model)

        model.setTrackName(channelInfo.`name`)
        model.initFromContext()
        model.getMidiInterfaceNames.find(i => i!= null && i.name == channelInfo.`midi-input`) match {
          case Some(midiIdentifier) =>
            model.setSelectedMidiInterface(midiIdentifier)
          case _ =>
        }
        model.getMidiVstSourceNames.find(_ == channelInfo.`vst-i`) match {
          case Some(vst) =>
            model.setSelectedMidiVst(vst)
          case _ =>
        }
        model.setTrackPianoEnabled(channelInfo.`piano-enabled`)
        model.setTrackPianoRollEnabled(channelInfo.`piano-roll-enabled`)

        // TODO Set Piano Range (Start -> ENd)
      }

    save
      .`save-state`
        .`mixer`
      .`bus-info`
      .foreach { busInfo =>
        val model = new BusMixModel(busInfo.`bus`)
        Context.mixerModel.addBusMix(model)

        model.setBusChannels(busInfo.`bus-mix`.map { busMix =>
          val busChannelModel = new BusChannelModel(busMix.`channel-id`)
          busChannelModel.setChannelAttenuation(busMix.`level`)
          Context.trackSetModel.getTrackSet.find(_.channel.id == busMix.`channel-id`) match {
            case Some(channelModel) =>
              busChannelModel.getChannelNameProperty.bind(channelModel.getTrackNameProperty)
            case _ =>
              throw new Exception(s"Reference to non-existing channel '${busMix.`channel-id`}' in Bus '${busInfo.`bus`}")
          }
          busChannelModel
        })
      }
  }
}
