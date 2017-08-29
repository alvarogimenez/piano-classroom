package sound.audio.channel

class ChannelController() extends ChannelOwner {
  var channels: List[Channel] = List.empty[Channel]

  def addChannel(channel: Channel): Unit = {
    channels = channels :+ channel
  }

  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]] = {
    channels.map(c => c.index -> c.pull(sampleRate, bufferSize)).toMap
  }
}
