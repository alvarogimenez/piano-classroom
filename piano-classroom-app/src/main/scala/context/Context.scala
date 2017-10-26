package context

import java.io.File
import javafx.beans.{InvalidationListener, Observable}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

import io.contracts._
import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.Serialization
import sound.audio.asio.AsioService
import sound.audio.channel.ChannelService
import sound.audio.mixer.{MixListener, MixerService}
import sound.midi.MidiService
import ui.controller.MainStageController
import ui.controller.component.PaletteColorButton
import ui.controller.mixer.MixerModel
import ui.controller.monitor.drawboard.{CanvasData, CanvasLine, DrawBoardCanvasModel}
import ui.controller.monitor.{MonitorModel, MonitorSource}
import ui.controller.track.TrackSetModel
import ui.renderer.GlobalRenderer

import scala.util.Try

object Context {
  implicit val formats: Formats =
    Serialization.formats(NoTypeHints) +
      new GlobalMonitorDrawBoardSettingsCanvasShapeSerializer

  var sessionSettings: SessionContract = readSessionSettings()

  val midiService = new MidiService()
  val channelService = new ChannelService()
  val mixerService = new MixerService(channelService)
  val asioService = new AsioService(mixerService)

  val trackSetModel = new TrackSetModel()
  val mixerModel = new MixerModel()
  val monitorModel = new MonitorModel()

  val globalRenderer = new GlobalRenderer()
  globalRenderer.startThread()

  var primaryStage: Stage = _

  var updateSessionDisabled = false

  mixerModel.addInvalidationListener(new InvalidationListener {
    override def invalidated(observable: Observable) = {
      println(mixerModel.dumpMix)
      mixerService.setFullMix(mixerModel.dumpMix)
    }
  })

  mixerService.addMixListener(new MixListener {
    override def handle(channelLevel: Map[String, Float], busLevel: Map[Int, Float]) = {
      mixerModel.handleMixOutput(channelLevel, busLevel)
    }
  })

  midiService.attach()

  sessionSettings.`audio-configuration` match {
    case Some(audioConfiguration) =>
      if(asioService.listDriverNames().contains(audioConfiguration.`driver-name`)) {
        println(s"Initialize ASIo Driver '${audioConfiguration.`driver-name`}' from Session Configuration")

        asioService.init(audioConfiguration.`driver-name`)

        val inputConfiguration =
          if(audioConfiguration.`channel-configuration`.input.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioService.getAvailableInputChannels())) {
              (0 until asioService.getAvailableInputChannels())
                .map { c =>
                  (c, audioConfiguration.`channel-configuration`.input.find(_.`channel-number` == c).exists(_.enabled))
                }
                .toMap
          } else {
            (0 until asioService.getAvailableInputChannels()).map( _ -> false).toMap
          }

        val outputConfiguration =
          if(audioConfiguration.`channel-configuration`.output.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioService.getAvailableOutputChannels())) {
            (0 until asioService.getAvailableOutputChannels())
              .map { c =>
                (c, audioConfiguration.`channel-configuration`.output.find(_.`channel-number` == c).exists(_.enabled))
              }
              .toMap
          } else {
            (0 until asioService.getAvailableOutputChannels()).map( _ -> false).toMap
          }

        println(audioConfiguration)
        println(s"Configure ASIO Buffers. Input [$inputConfiguration], Output [$outputConfiguration]")

        asioService.configureChannelBuffers(inputConfiguration, outputConfiguration)
        asioService.start()
      } else {
        println(s"Driver '${audioConfiguration.`driver-name`}' is not available in the System. Will initialize no ASIO Driver")
      }
    case _ =>
      println(s"No ASIO Driver found on configuration. Skiping ASIO Driver initialization phase")
  }

  def loadControllerDependantSettings(controller: MainStageController): Unit = {
    updateSessionDisabled = true

    sessionSettings
      .`global`
      .foreach { globalSettings =>
        // IO Configuration
        globalSettings.`io` match {
          case Some(ioSettings) =>
            ioSettings.`last-opened-file`.foreach { lastOpenedFile =>
              loadFile(new File(lastOpenedFile))
            }
          case _ =>
        }

        // Monitor Configuration
        globalSettings.`monitor` match {
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
      }

    updateSessionDisabled = false
  }
}

