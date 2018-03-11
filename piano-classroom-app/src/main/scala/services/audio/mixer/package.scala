package services.audio

package object mixer {
  case class BusMix(bus: Int, level: Float, channelMix: List[ChannelMix]) {
    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case o: BusMix => this.bus == o.bus
        case _ => false
      }
    }
  }
  case class ChannelMix(channel: String, mix: Float) {
    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case o: ChannelMix => this.channel == o.channel
        case _ => false
      }
    }
  }
}
