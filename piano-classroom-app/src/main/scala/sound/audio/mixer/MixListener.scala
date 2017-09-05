package sound.audio.mixer


trait MixListener {
  def handle(channelLevel: Map[String, Float], busLevel: Map[Int, Float]): Unit
}
