package sound.audio.channel

import javafx.beans.property.{ListProperty, SimpleListProperty}
import javafx.collections.{FXCollections, ObservableList}

import scala.collection.JavaConversions._
import scala.collection.mutable.Seq

class ChannelService extends ChannelOwner {
  private val channels: Seq[Channel] = Seq.empty[Channel]
  private val channels_propery_ol: ObservableList[Channel] = FXCollections.observableArrayList[Channel](channels)
  private val channels_property: SimpleListProperty[Channel] = new SimpleListProperty[Channel](channels_propery_ol)

  def getChannels: List[Channel] = channels_property.toList
  def addChannel(c: Channel): Unit = channels_property.add(c)
  def removeChannel(c: Channel): Unit = channels_property.remove(c)
  def clearChannels: Unit = channels_property.clear()
  def getChannelsProperty: ListProperty[Channel] = channels_property

  def closeAndRemoveChannels(): Unit = {
    channels.foreach(_.close())
    channels_property.clear()
  }

  def pull(sampleRate: Double, bufferSize: Int): Map[String, Array[Float]] = {
    channels.map(c => c.getId -> c.pull(sampleRate, bufferSize)).toMap
  }
}
