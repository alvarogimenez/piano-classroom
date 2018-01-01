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

  var applicationSession: ApplicationSessionContract = readApplicationSession()
  var projectSession: ProjectSessionContract =
    readProjectSession(
      source = applicationSession.`global`.flatMap(_.`io`.flatMap(_.`last-opened-file`))
    )

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

  var updateProjectSessionDisabled = false

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

  applicationSession.`audio-configuration` match {
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
    applicationSession
      .`global`
      .foreach { globalSettings =>
        // IO Configuration
        globalSettings.`io` match {
          case Some(ioSettings) =>
            ioSettings.`last-opened-file`.foreach { lastOpenedFile =>
              Context.projectSession = context.readProjectSession(Some(lastOpenedFile))
              context.loadProjectSession(controller)
            }
          case _ =>
        }
      }
  }
}

