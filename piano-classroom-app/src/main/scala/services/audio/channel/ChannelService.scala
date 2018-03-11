package services.audio.channel

import javafx.beans.{InvalidationListener, Observable}
import javafx.beans.property.{ListProperty, SimpleListProperty}
import javafx.collections.{FXCollections, ObservableList}

import scala.collection.JavaConversions._

class ChannelService extends ChannelOwner {
  private var _channels: Seq[Channel] = Seq.empty[Channel]
  private val channels_property_ol: ObservableList[Channel] = FXCollections.observableArrayList[Channel]()
  private val channels_property: SimpleListProperty[Channel] = new SimpleListProperty[Channel](channels_property_ol)

  def getChannels: List[Channel] = channels_property.toList
  def addChannel(c: Channel): Unit = channels_property.add(c)
  def removeChannel(c: Channel): Unit = channels_property.remove(c)
  def clearChannels: Unit = channels_property.clear()
  def getChannelsProperty: ListProperty[Channel] = channels_property

  def closeAndRemoveChannels(): Unit = {
    _channels.foreach(_.close())
    channels_property.clear()
  }

  channels_property.addListener(new InvalidationListener {
    override def invalidated(observable: Observable) = {
      _channels = channels_property_ol.toList
    }
  })

  def pull(sampleRate: Double, bufferSize: Int): Map[String, Array[Float]] = {
    _channels.map(c => c.getId -> c.pull(sampleRate, bufferSize)).toMap
  }
}
