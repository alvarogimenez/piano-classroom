package sound.audio.channel


trait Channel {
  val id: String
  def pull(sampleRate: Double, bufferSize: Int): Array[Float]
}


