import java.io.{File, FileNotFoundException, PrintWriter}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

import io.contracts._
import io.{fromJson, toJson}
import sound.audio.channel.MidiChannel
import ui.controller.MainStageController
import ui.controller.component.PaletteColorButton
import ui.controller.mixer.{BusChannelModel, BusMixModel}
import ui.controller.monitor.MonitorSource
import ui.controller.monitor.drawboard.{CanvasData, CanvasLine, DrawBoardCanvasModel}
import ui.controller.track.TrackModel
import util.{KeyboardNote, MusicNote}

import scala.io.Source
import scala.util.{Failure, Success, Try}

package object context {
  def readApplicationSession(): ApplicationSessionContract = {
    import Context._

    val sessionContract =
      Try(Source.fromFile("session.json").mkString)
        .map(fromJson[ApplicationSessionContract])
        .recoverWith {
          case e: FileNotFoundException =>
            println(s"No session.json file found in the working directory. Creating an empty one")
            val emptySession = initializeEmptySesionContract()
            Try(toJson(emptySession))
              .map { source =>
                val w = new PrintWriter(new File("session.json"))
                w.write(source)
                w.close()
              }
              .map(_ => emptySession)
              .recoverWith {
                case e: Exception =>
                  throw new Exception(s"Error while writing session.json: '${e.getMessage}'")
              }
          case e: Exception =>
            throw new Exception(s"Error while reading session.json: '${e.getMessage}'")
        }

    sessionContract match {
      case Success(contract) => contract
      case Failure(exception) => throw exception
    }
  }

  def readProjectSession(source: Option[String]): ProjectSessionContract = {
    import Context._

    source match {
      case Some(s) =>
        val sessionContract =
          Try(Source.fromFile(s).mkString)
            .map(fromJson[ProjectSessionContract])
            .recoverWith {
              case e: FileNotFoundException =>
                throw new Exception(s"Filed to find file '$s'")
              case e: Exception =>
                throw new Exception(s"Filed to open file '$s'")
            }

        sessionContract match {
          case Success(contract) => contract
          case Failure(exception) => throw exception
        }
      case None =>
        ProjectSessionContract(
          `version`= "1.0.0",
          `save-state` = SaveState(
            `tracks` = SaveTracks(
              `channel-info` = List.empty
            ),
            `mixer` = SaveMixer(
              `bus-info` = List.empty
            ),
            `monitor` = None
          )
        )
    }

  }

  def writeApplicationSessionSettings(session: ApplicationSessionContract): Unit = {
    import Context._

    if(!Context.updateProjectSessionDisabled) {
      try {
        val w = new PrintWriter(new File("session.json"))
        w.write(toJson(session))
        w.close()
      } catch {
        case e: Exception =>
          println(s"Error writing session.json file: '${e.getMessage}'")
      }
    }
  }

  def writeProjectSessionSettings(session: ProjectSessionContract): Unit = {
    import Context._

    Context.applicationSession.`global` match {
      case Some(global) =>
        global.`io` match {
          case Some(io) =>
            io.`last-opened-file` foreach { lastOpenedFile =>
              if(!Context.updateProjectSessionDisabled) {
                try {
                  val w = new PrintWriter(new File(lastOpenedFile))
                  w.write(toJson(session))
                  w.close()
                } catch {
                  case e: Exception =>
                    println(s"Error writing '$lastOpenedFile' file: '${e.getMessage}'")
                }
              }
            }
          case None =>
        }
      case None =>
    }
  }

  def loadProjectSession(controller: MainStageController): Unit = {
    Context.updateProjectSessionDisabled = true

    Context.trackSetModel.clear()
    Context.mixerModel.clear()

    Context.mixerService.clearListeners
    Context.mixerService.clearMix
    Context.channelService.closeAndRemoveChannels()

    Context.projectSession
      .`save-state`
      .`tracks`
      .`channel-info`
      .foreach { channelInfo =>
        println(s"Creating channel '${channelInfo.`name`}' with id '${channelInfo.`id`}'")

        val midiChannel = new MidiChannel(channelInfo.`id`)
        val model = new TrackModel(midiChannel)

        Context.channelService.addChannel(midiChannel)
        Context.trackSetModel.addTrack(model)

        model.setTrackName(channelInfo.`name`)
        model.initFromContext()
        channelInfo.`midi-input`.foreach { midiInput =>
          model.getMidiInterfaceNames.find(i => i!= null && i.name == midiInput) match {
            case Some(midiIdentifier) =>
              model.setSelectedMidiInterface(midiIdentifier)
            case _ =>
          }
        }

        channelInfo.`vst-i`.foreach { vst =>
          model.getMidiVstSourceNames.find(_ == vst) match {
            case Some(v) =>
              model.setSelectedMidiVst(v)
            case _ =>
          }
        }

        model.setTrackPianoEnabled(channelInfo.`piano-enabled`)
        model.setTrackPianoRollEnabled(channelInfo.`piano-roll-enabled`)

        model.setTrackPianoStartNote(
          KeyboardNote(
            note = MusicNote.withName(channelInfo.`piano-range-start`.`note`),
            index = channelInfo.`piano-range-start`.index
          )
        )

        model.setTrackPianoEndNote(
          KeyboardNote(
            note = MusicNote.withName(channelInfo.`piano-range-end`.`note`),
            index = channelInfo.`piano-range-end`.index
          )
        )
      }

    Context.projectSession
      .`save-state`
      .`mixer`
      .`bus-info`
      .foreach { busInfo =>
        val model = new BusMixModel(busInfo.`bus`)
        Context.mixerModel.addBusMix(model)

        model.setBusAttenuation(busInfo.`master-level`)
        model.setBusChannels(busInfo.`bus-mix`.map { busMix =>
          val busChannelModel = new BusChannelModel(busMix.`channel-id`)
          busChannelModel.setChannelAttenuation(busMix.`level`.getOrElse(Double.NegativeInfinity))
          println(s"Set channel mix ${busMix.`level`.getOrElse(Double.NegativeInfinity)} to bus ${busMix.`channel-id`}")
          Context.trackSetModel.getTrackSet.find(_.channel.id == busMix.`channel-id`) match {
            case Some(channelModel) =>
              busChannelModel.getChannelNameProperty.bind(channelModel.getTrackNameProperty)
            case _ =>
              throw new Exception(s"Reference to non-existing channel '${busMix.`channel-id`}' in Bus '${busInfo.`bus`}")
          }
          busChannelModel
        })
      }

    // Monitor Configuration
    Context.projectSession
      .`save-state`
      .`monitor` match {
      case Some(monitorSettings) =>
        // Configure global Monitor Settings
        if(monitorSettings.`fullscreen`) {
          controller.selectMonitorSourceWithIndex(monitorSettings.`source-index`)
          controller.goMonitorFullScreen()
        }
        // Configure active view
        val activeView = monitorSettings.`active-view`.flatMap(v => Try(MonitorSource.withName(v)).toOption)
        activeView
          .foreach { view =>
            controller.selectMonitorView(view)
          }
        // Configure Camera Settings
        monitorSettings.`camera-settings`.`source` match {
          case Some(selectedCameraSource) =>
            val webCamSource = Context.monitorModel.monitorWebCamModel.getSources.find(w => w != null && w.name == selectedCameraSource)
            webCamSource match {
              case Some(w) =>
                Context.monitorModel.monitorWebCamModel.setSelectedSource(w)
              case _ =>
            }
          case _ =>
        }
        // Configure Note Display
        monitorSettings.`camera-settings`.`note-display` match {
          case Some(noteDisplay) =>
            noteDisplay.`display` match {
              case "FixedDo" => Context.monitorModel.monitorWebCamModel.setDisplayNoteInFixedDo(true)
              case "English" => Context.monitorModel.monitorWebCamModel.setDisplayNoteInEnglish(true)
              case "NoDisplay" => Context.monitorModel.monitorWebCamModel.setDisplayNoteDisabled(true)
              case _ =>
            }
            noteDisplay.`source-track-id` match {
              case Some(id) =>
                println(Context
                  .monitorModel
                  .monitorWebCamModel
                  .getTrackNoteSources)
                Context
                  .monitorModel
                  .monitorWebCamModel
                  .getTrackNoteSources
                  .find(s => s != null && s.id == id)
                  .foreach { source =>
                    Context.monitorModel.monitorWebCamModel.setTrackNoteSelectedSource(source)
                  }
              case _ =>
            }

          case _ =>
        }
        // Drawboard Settings
        monitorSettings.`draw-board-settings`.`pens` match {
          case Some(pens) =>
            Context.monitorModel.monitorDrawBoardModel.setAvailableColorButtons(
              pens
                .map { pen =>
                  new PaletteColorButton(
                    new Color(
                      pen.`r` / 255.0,
                      pen.`g` / 255.0,
                      pen.`b` / 255.0,
                      1.0
                    ),
                    pen.`size` / 1000
                  )
                }
            )
          case _ =>
        }

        monitorSettings.`draw-board-settings`.`canvas` match {
          case Some(canvas) =>
            Context.monitorModel.monitorDrawBoardModel.setDrawBoardCanvasModels(
              canvas
                .map { c =>
                  val m = new DrawBoardCanvasModel()
                  m.setCanvasData(CanvasData(
                    name = c.`name`,
                    aspectRatio = c.`aspect-ratio`,
                    fullscreenViewport = new Rectangle(0, 0, 100, 100),
                    shapes =
                      c.`shapes`
                        .map {
                          case x: GlobalMonitorDrawBoardSettingsCanvasLine =>
                            CanvasLine(
                              x.`id`,
                              CanvasLine.pathFromString(x.`path`),
                              x.`size`,
                              new Color(
                                x.`color`.`r` / 255.0,
                                x.`color`.`g` / 255.0,
                                x.`color`.`b` / 255.0,
                                1.0
                              )
                            )
                        }.toSet
                  ))
                  m
                }
            )
          case _ =>
        }
        monitorSettings.`draw-board-settings`.`selected-canvas-name` match {
          case Some(selectedCanvasName) =>
            Context
              .monitorModel
              .monitorDrawBoardModel
              .getDrawBoardCanvasModels
              .find(_.getCanvasData.name == selectedCanvasName)
              .foreach { model =>
                Context
                  .monitorModel
                  .monitorDrawBoardModel
                  .setSelectedDrawBoardCanvasModel(model)
              }
          case _ =>
        }
      case _ =>
    }

    Context.updateProjectSessionDisabled = false
  }
}
