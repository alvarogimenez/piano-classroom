package context

import javafx.beans.{InvalidationListener, Observable}
import javafx.stage.Stage

import io.contracts._
import sound.audio.asio.AsioService
import sound.audio.channel.ChannelService
import sound.audio.mixer.{MixListener, MixerService}
import sound.midi.MidiService
import ui.controller.mixer.MixerModel
import ui.controller.monitor.MonitorModel
import ui.controller.track.TrackSetModel
import ui.renderer.GlobalRenderer

object Context {
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

  var pianoMock = new Thread() {

  }

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

  var primaryStage: Stage = _


}
