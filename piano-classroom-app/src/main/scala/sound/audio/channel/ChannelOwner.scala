package sound.audio.channel

trait ChannelOwner {
  def channels: List[Channel]
  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]]
}
