package sound.audio.mixer

import sound.audio.channel.ChannelOwner


class MixerController(val channelOwner: ChannelOwner) extends MixerOwner {
  var mixByOutputChannel: Map[Int, Set[ChannelMix]] = Map.empty

  def getMixOfOutput(output: Int): Set[ChannelMix] =
    mixByOutputChannel.getOrElse(output, Set.empty)

  def setChannelInOutput(channelMix: ChannelMix, output: Int): Unit =
    mixByOutputChannel = mixByOutputChannel.updated(output, mixByOutputChannel.getOrElse(output, Set.empty) + channelMix)

  def removeChannelFromAllOutputs(channelIndex: Int): Unit =
    mixByOutputChannel =
      mixByOutputChannel
        .map {case (key, value) =>
          (key, value.filterNot(_.channel == channelIndex))
        }

  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]] = {
    val channelData = channelOwner.pull(sampleRate, bufferSize)
    mixByOutputChannel
        .map { case (key, mixes) =>
          (
            key,
            MixerUtils.mix(mixes.map(m => (channelData.getOrElse(m.channel, Array.fill[Float](bufferSize)(0)), m.mix)).toList)
          )
        }
  }
}
