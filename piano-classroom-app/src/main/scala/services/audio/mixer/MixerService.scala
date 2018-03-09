package services.audio.mixer

import services.audio.channel.ChannelOwner


class MixerService(val channelOwner: ChannelOwner) extends MixerOwner {
  final val DB_MIN_PRECISSION = 0.01

  var mixByOutputChannel: List[BusMix] = List.empty
  var mixListeners: List[MixListener] = List.empty
  var lastMixListenerUpdate = System.currentTimeMillis()

  def setFullMix(m: List[BusMix]): Unit = mixByOutputChannel = m

  def addMixListener(m: MixListener): Unit = mixListeners = mixListeners :+ m

  def clearMix: Unit = mixByOutputChannel = List.empty
  def clearListeners: Unit = mixListeners = List.empty

  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]] = {
    val channelData = channelOwner.pull(sampleRate, bufferSize)
    val mixOutputByChannel = mixByOutputChannel
        .map { busMix =>
          (
            busMix.bus,
            MixerUtils.mix(busMix.channelMix.map(m => (channelData.getOrElse(m.channel, Array.fill[Float](bufferSize)(0)), m.mix)))
          )
        }
        .toMap

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
