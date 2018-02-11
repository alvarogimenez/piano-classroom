import java.io.{File, FileNotFoundException, PrintWriter}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

import io.contracts._
import io.{fromJson, toJson}
import sound.audio.channel.MidiChannel
import sound.midi.{MidiInterfaceIdentifier, MidiVstSource}
import ui.controller.MainStageController
import ui.controller.component.PaletteColorButton
import ui.controller.mixer._
import ui.controller.monitor.{MonitorSource, TrackProfile, TrackProfileInfo}
import ui.controller.monitor.drawboard.{CanvasData, CanvasLine, DrawBoardCanvasModel}
import ui.controller.track.TrackModel
import util.KeyboardLayoutUtils.{KeyBoundingBox, KeyboardLayout}
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
              `channel-info` = List.empty,
              `channel-profiles` = List.empty
            ),
            `mixer` = SaveMixer(
              `bus-info` = List.empty,
              `mixer-profiles`= List.empty
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
    if(!Context.updateProjectSessionDisabled) {
      Context.projectSession.set(session)
      Context.projectSessionDirty.set(true)
    }
  }

  def loadControllerDependantSettings(controller: MainStageController): Unit = {
    Context.
      applicationSession
      .get()
      .`global`
      .foreach { globalSettings =>
        // IO Configuration
        globalSettings.`io` match {
          case Some(ioSettings) =>
            ioSettings.`last-opened-file`.foreach { lastOpenedFile =>
              Context.projectSession.set(context.readProjectSession(Some(lastOpenedFile)))
              context.loadProjectSession(controller)
            }
          case _ =>
        }
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
      .get()
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
          model.getMidiVstSources.find(i => i != null && i.path == vst) match {
            case Some(v) =>
              model.setSelectedMidiVst(v)
            case _ =>
          }
        }

        channelInfo.`vst-properties`.foreach { vstProperties =>
          vstProperties.foreach { case (key, value) =>
            midiChannel.vstPlugin.flatMap(_.vst).foreach { vst =>
              vst.setParameter(key.toInt, value.toFloat)
            }
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

    Context.trackSetModel.setTrackProfiles(
      Context.projectSession
        .get()
        .`save-state`
        .`tracks`
        .`channel-profiles`
        .map { channelProfile =>
          TrackProfile(
            name = channelProfile.`name`,
            color = Color.web(channelProfile.`color`),
            tracks = channelProfile.`channel-profiles`.map { channelProfileInfo =>
              TrackProfileInfo(
                id = channelProfileInfo.id,
                midiInput = channelProfileInfo.`midi-input`.map {channelProfileInfoMidiInput =>
                  MidiInterfaceIdentifier(
                    name = channelProfileInfoMidiInput.`name`
                  )
                },
                vstInput = channelProfileInfo.`vst-input`.map { channelProfileInfoVstInput =>
                  MidiVstSource(
                    path = channelProfileInfoVstInput.`path`,
                    name = channelProfileInfoVstInput.`name`
                  )
                },
                vstProperties = channelProfileInfo.`vst-properties`,
                pianoEnabled = channelProfileInfo.`piano-enabled`,
                pianoRollEnabled = channelProfileInfo.`piano-roll-enabled`,
                pianoRangeStart = KeyboardNote(
                  note = MusicNote.withName(channelProfileInfo.`piano-range-start`.`note`),
                  index = channelProfileInfo.`piano-range-start`.index
                ),
                pianoRangeEnd = KeyboardNote(
                  note = MusicNote.withName(channelProfileInfo.`piano-range-end`.`note`),
                  index = channelProfileInfo.`piano-range-end`.index
                )
              )
            }
          )
        }
    )

    Context.projectSession
      .get()
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

        model.setBusMixProfiles(busInfo.`bus-profiles`.map { busMixProfile =>
          BusMixProfile(
            name = busMixProfile.name,
            color = Color.web(busMixProfile.`color`),
            busLevel = busMixProfile.`bus-level`.toFloat,
            busMixes = busMixProfile.`bus-mixes`.map { busMix =>
              BusChannelMixProfile(
                channel = busMix.`channel`,
                mix = busMix.`mix`.toFloat,
                active = busMix.`active`,
                solo = busMix.`solo`
              )
            }
          )
        })
      }

    Context.mixerModel.setMixerProfiles(
      Context.projectSession
        .get()
        .`save-state`
        .`mixer`
        .`mixer-profiles`
        .map { mixerProfile =>
          MixerProfile(
            name = mixerProfile.name,
            color = Color.web(mixerProfile.`color`),
            busMixes = mixerProfile.`bus-profiles`.map { busProfile =>
              BusProfile(
                bus = busProfile.`bus`,
                busLevel = busProfile.`bus-level`.toFloat,
                busMixes = busProfile.`bus-mixes`.map { busMix =>
                  BusChannelMixProfile(
                    channel = busMix.`channel`,
                    mix = busMix.`mix`.toFloat,
                    active = busMix.`active`,
                    solo = busMix.`solo`
                  )
                }
              )
            }
          )
        }
    )

    // Monitor Configuration
    Context.projectSession
      .get()
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
        // Configure sustain dispay
        monitorSettings.`camera-settings`.`sustain-active` match {
          case Some(sustainActive) =>
            Context
                .monitorModel
                .monitorWebCamModel
                .setSustainActive(sustainActive)
          case _ =>
        }
        // Configure highlighter
        monitorSettings.`camera-settings`.`highlighter-enabled` match {
          case Some(highlighterEnabled) =>
            Context
              .monitorModel
              .monitorWebCamModel
              .setHighlightEnabled(highlighterEnabled)
          case _ =>
        }

        monitorSettings.`camera-settings`.`highlighter-subtractive` match {
          case Some(highlighterSubtractive) =>
            Context
              .monitorModel
              .monitorWebCamModel
              .setHighlightSubtractive(highlighterSubtractive)
          case _ =>
        }

        monitorSettings.`camera-settings`.`highlighter-subtractive-sensibility` match {
          case Some(highlighterSubtractiveSensibility) =>
            Context
              .monitorModel
              .monitorWebCamModel
              .setHighlightSubtractiveSensibility(highlighterSubtractiveSensibility)
          case _ =>
        }

        monitorSettings.`camera-settings`.`keyboard-layout` match {
          case Some(keyboardLayout) =>
            Context
              .monitorModel
              .monitorWebCamModel
              .setKeyboardLayout(
                KeyboardLayout(
                  brightnessThreshold = keyboardLayout.`brightness-threshold`,
                  smoothAverage = keyboardLayout.`smooth-average`,
                  cutY = keyboardLayout.`cut-y`,
                  layout = keyboardLayout.`layout-data`.map { layoutData =>
                    KeyBoundingBox(
                      key = KeyboardNote(MusicNote.withName(layoutData.`note`), layoutData.`note-index`),
                      left = layoutData.`left`,
                      right = layoutData.`right`,
                      top = layoutData.`top`,
                      bottom = layoutData.`bottom`,
                      mask = layoutData.`mask`.map { mask =>
                        mask.split("\\|").map(_.split("\\;").map(_.toInt))
                      }
                    )
                  }
                )
              )
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
