package sound.audio.channel


trait Channel {
  val id: String
  def pull(sampleRate: Double, bufferSize: Int): Array[Float]

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case x: Channel => x.id == id
      case _ => false
    }
  }
}


