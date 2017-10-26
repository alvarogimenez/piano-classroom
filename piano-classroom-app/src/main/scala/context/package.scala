import java.io.{File, FileNotFoundException, PrintWriter}

import io.contracts._
import io.{fromJson, toJson}
import sound.audio.channel.MidiChannel
import ui.controller.mixer.{BusChannelModel, BusMixModel}
import ui.controller.track.TrackModel

import scala.io.Source
import scala.util.{Failure, Success, Try}

package object context {
  def readSessionSettings(): SessionContract = {
    import Context._

    val sessionContract =
      Try(Source.fromFile("session.json").mkString)
        .map(fromJson[SessionContract])
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

  def writeSessionSettings(session: SessionContract): Unit = {
    import Context._

    if(!Context.updateSessionDisabled) {
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

  def loadFile(file: File): Unit = {
    import Context._

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
      .foreach { save =>
        Context.trackSetModel.clear()
        Context.mixerModel.clear()

        Context.mixerService.clearListeners
        Context.mixerService.clearMix
        Context.channelService.closeAndRemoveChannels()

        save
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
              busChannelModel.setChannelAttenuation(busMix.`level`.getOrElse(Double.NegativeInfinity))
              Context.trackSetModel.getTrackSet.find(_.channel.id == busMix.`channel-id`) match {
                case Some(channelModel) =>
                  busChannelModel.getChannelNameProperty.bind(channelModel.getTrackNameProperty)
                case _ =>
                  throw new Exception(s"Reference to non-existing channel '${busMix.`channel-id`}' in Bus '${busInfo.`bus`}")
              }
              busChannelModel
            })
          }

        val ioConfiguration =
          Context
            .sessionSettings
            .`global`
            .flatMap(_.`io`)
            .getOrElse(GlobalIoConfiguration())
            .copy(`last-opened-file` = Some(file.getAbsolutePath))

        context.writeSessionSettings(
          Context.sessionSettings.copy(
            `global` =
              Some(
                Context.sessionSettings.`global`
                  .getOrElse(GlobalConfiguration())
                  .copy(`io` = Some(ioConfiguration))
              )
          )
        )
      }
  }
}
