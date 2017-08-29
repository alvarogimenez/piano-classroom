package sound.audio

package object mixer {
  case class ChannelMix(channel: Int, mix: Float) {
    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case o: ChannelMix => this.channel == o.channel
        case _ => false
      }
    }
  }
}
