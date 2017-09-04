package sound.audio.channel

class ChannelService() extends ChannelOwner {
  var channels: List[Channel] = List.empty[Channel]

  def addChannel(channel: Channel): Unit = {
    channels = channels :+ channel
  }

  def pull(sampleRate: Double, bufferSize: Int): Map[String, Array[Float]] = {
    channels.map(c => c.id -> c.pull(sampleRate, bufferSize)).toMap
  }
}
