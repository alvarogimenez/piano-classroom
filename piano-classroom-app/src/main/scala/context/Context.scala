package context

import javafx.stage.Stage

import io.contracts._
import sound.audio.asio.AsioController
import sound.audio.channel.ChannelController
import sound.audio.mixer.MixerController
import sound.midi.MidiController

object Context {
  var sessionSettings: SessionContract = readSessionSettings()

  val midiController = new MidiController()
  val channelController = new ChannelController()
  val mixerController = new MixerController(channelController)
  val asioController = new AsioController(mixerController)

  sessionSettings.`audio-configuration` match {
    case Some(audioConfiguration) =>
      if(asioController.listDriverNames().contains(audioConfiguration.`driver-name`)) {
        println(s"Initialize ASIo Driver '${audioConfiguration.`driver-name`}' from Session Configuration")

        asioController.init(audioConfiguration.`driver-name`)

        val inputConfiguration =
          if(audioConfiguration.`channel-configuration`.input.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioController.getAvailableInputChannels())) {
              (0 until asioController.getAvailableInputChannels())
                .map { c =>
                  (c, audioConfiguration.`channel-configuration`.input.find(_.`channel-number` == c).exists(_.enabled))
                }
                .toMap
          } else {
            (0 until asioController.getAvailableInputChannels()).map( _ -> false).toMap
          }

        val outputConfiguration =
          if(audioConfiguration.`channel-configuration`.output.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioController.getAvailableOutputChannels())) {
            (0 until asioController.getAvailableOutputChannels())
              .map { c =>
                (c, audioConfiguration.`channel-configuration`.output.find(_.`channel-number` == c).exists(_.enabled))
              }
              .toMap
          } else {
            (0 until asioController.getAvailableOutputChannels()).map( _ -> false).toMap
          }

        println(audioConfiguration)
        println(s"Configure ASIO Buffers. Input [$inputConfiguration], Output [$outputConfiguration]")

        asioController.configureChannelBuffers(inputConfiguration, outputConfiguration)
        asioController.start()
      } else {
        println(s"Driver '${audioConfiguration.`driver-name`}' is not available in the System. Will initialize no ASIO Driver")
      }
    case _ =>
      println(s"No ASIO Driver found on configuration. Skiping ASIO Driver initialization phase")
  }

  var primaryStage: Stage = _


}
