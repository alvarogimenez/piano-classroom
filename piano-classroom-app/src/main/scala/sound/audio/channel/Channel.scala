package sound.audio.channel


trait Channel {
  val index: Int
  def pull(sampleRate: Double, bufferSize: Int): Array[Float]
}


