package context

import javafx.beans.property.{BooleanProperty, ObjectProperty, SimpleBooleanProperty, SimpleObjectProperty}
import javafx.beans.{InvalidationListener, Observable}
import javafx.stage.Stage

import io.autoSave.AutoSave
import io.contracts._
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints}
import services.audio.asio.AsioService
import services.audio.channel.ChannelService
import services.audio.mixer.{MixListener, MixerService}
import services.audio.playback.PlaybackService
import services.midi.MidiService
import ui.controller.mixer.MixerModel
import ui.controller.monitor.MonitorModel
import ui.controller.recording.RecordingModel
import ui.controller.track.TrackSetModel
import ui.renderer.GlobalRenderer

object Context {
  implicit val formats: Formats =
    Serialization.formats(NoTypeHints) +
      new GlobalMonitorDrawBoardSettingsCanvasShapeSerializer

  val applicationSession: ObjectProperty[ApplicationSessionContract] = new SimpleObjectProperty[ApplicationSessionContract]()
  val projectSession: ObjectProperty[ProjectSessionContract] =  new SimpleObjectProperty[ProjectSessionContract]()
  val projectSessionDirty: BooleanProperty = new SimpleBooleanProperty()
  val projectSessionSaving: BooleanProperty = new SimpleBooleanProperty()

  applicationSession.set(readApplicationSession())
  projectSession.set(readProjectSession(
    source = applicationSession.get().`global`.flatMap(_.`io`.flatMap(_.`last-opened-file`))
  ))
  projectSessionDirty.set(false)
  projectSessionSaving.set(false)

  val midiService = new MidiService()
  val channelService = new ChannelService()
  val mixerService = new MixerService(channelService)
  val asioService = new AsioService(mixerService)
  val playbackService = new PlaybackService()

  val trackSetModel = new TrackSetModel()
  val recordingModel = new RecordingModel()
  val mixerModel = new MixerModel()
  val monitorModel = new MonitorModel()

  val globalRenderer = new GlobalRenderer()
  globalRenderer.startThread()

  val autoSave = new AutoSave()
  autoSave.startThread()

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

  applicationSession.get().`audio-configuration` match {
    case Some(audioConfiguration) =>
      if(asioService.listDriverNames().contains(audioConfiguration.`driver-name`)) {
        println(s"Initialize ASIo Driver '${audioConfiguration.`driver-name`}' from Session Configuration")

        try {
          asioService.init(audioConfiguration.`driver-name`)

          val inputConfiguration =
            if (audioConfiguration.`channel-configuration`.input.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioService.getAvailableInputChannels())) {
              (0 until asioService.getAvailableInputChannels())
                .map { c =>
                  (c, audioConfiguration.`channel-configuration`.input.find(_.`channel-number` == c).exists(_.enabled))
                }
                .toMap
            } else {
              (0 until asioService.getAvailableInputChannels()).map(_ -> false).toMap
            }

          val outputConfiguration =
            if (audioConfiguration.`channel-configuration`.output.forall(ace => ace.`channel-number` >= 0 && ace.`channel-number` < asioService.getAvailableOutputChannels())) {
              (0 until asioService.getAvailableOutputChannels())
                .map { c =>
                  (c, audioConfiguration.`channel-configuration`.output.find(_.`channel-number` == c).exists(_.enabled))
                }
                .toMap
            } else {
              (0 until asioService.getAvailableOutputChannels()).map(_ -> false).toMap
            }

          println(audioConfiguration)
          println(s"Configure ASIO Buffers. Input [$inputConfiguration], Output [$outputConfiguration]")

          asioService.configureChannelBuffers(inputConfiguration, outputConfiguration)
          asioService.start()
        } catch {
          case e: Exception =>
            println(s"Exception while initializing Audio Driver '${audioConfiguration.`driver-name`}': " + e.getMessage)
            e.printStackTrace()
        }
      } else {
        println(s"Driver '${audioConfiguration.`driver-name`}' is not available in the System. Will initialize no ASIO Driver")
      }
    case _ =>
      println(s"No ASIO Driver found on configuration. Skiping ASIO Driver initialization phase")
  }
}

