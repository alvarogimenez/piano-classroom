package services.audio.channel

trait ChannelOwner {
  def pull(sampleRate: Double, bufferSize: Int): Map[String, Array[Float]]
}
