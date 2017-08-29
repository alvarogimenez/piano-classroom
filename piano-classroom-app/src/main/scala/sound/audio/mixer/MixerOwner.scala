package sound.audio.mixer


trait MixerOwner {
  def pull(sampleRate: Double, bufferSize: Int): Map[Int, Array[Float]]
}
