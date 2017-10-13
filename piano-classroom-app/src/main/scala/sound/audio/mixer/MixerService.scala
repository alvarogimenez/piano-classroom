package sound.audio.mixer

import sound.audio.channel.ChannelOwner


class MixerService(val channelOwner: ChannelOwner) extends MixerOwner {
  final val DB_MIN_PRECISSION = 0.01

  var mixByOutputChannel: Map[Int, Set[ChannelMix]] = Map.empty
  var mixListeners: List[MixListener] = List.empty
  var lastMixListenerUpdate = System.currentTimeMillis()

  def getMixOfOutput(output: Int): Set[ChannelMix] =
    mixByOutputChannel.getOrElse(output, Set.empty)

  def setChannelInOutput(channelMix: ChannelMix, output: Int): Unit =
    mixByOutputChannel = mixByOutputChannel.updated(output, mixByOutputChannel.getOrElse(output, Set.empty) + channelMix)

  def setFullMix(m: Map[Int, Set[ChannelMix]]): Unit = mixByOutputChannel = m

  def removeChannelFromAllOutputs(channelIndex: Int): Unit =
    mixByOutputChannel =
      mixByOutputChannel
        .map {case (key, value) =>
          (key, value.filterNot(_.channel == channelIndex))
        }

  def addMixListener(m: MixListener): Unit = mixListeners = mixListeners :+ m

  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]] = {
    val channelData = channelOwner.pull(sampleRate, bufferSize)
    val mixOutputByChannel = mixByOutputChannel
        .map { case (key, mixes) =>
          (
            key,
            MixerUtils.mix(mixes.map(m => (channelData.getOrElse(m.channel, Array.fill[Float](bufferSize)(0)), m.mix)).toList)
          )
        }

//    val currentTime = System.currentTimeMillis()
//    if(currentTime - lastMixListenerUpdate > 200) {
//      lastMixListenerUpdate = currentTime
//      mixListeners.foreach(_.handle(
//        channelLevel = channelData.mapValues(x => 20* Math.log10(minDouble(x.max)).toFloat),
//        busLevel = mixOutputByChannel.mapValues(x => 20* Math.log10(minDouble(x.max)).toFloat)
//      ))
//    }

    mixOutputByChannel
  }

  private def minDouble(x: Double) = if(x < DB_MIN_PRECISSION) 0.0 else x
}
