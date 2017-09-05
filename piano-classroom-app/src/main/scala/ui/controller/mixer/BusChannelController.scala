package ui.controller.mixer

import javafx.beans.property.{SimpleDoubleProperty, SimpleStringProperty}
import javafx.beans.{InvalidationListener, Observable}
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane

import ui.controller.component.Fader

class BusChannelModel(val id: String) {
  var invalidationListeners: Set[InvalidationListener] = Set.empty[InvalidationListener]
  var channel_name: SimpleStringProperty = new SimpleStringProperty()
  var channel_attenuation: SimpleDoubleProperty = new SimpleDoubleProperty()
  val channel_level_db: SimpleDoubleProperty = new SimpleDoubleProperty()

  def getChannelName: String = channel_name.get()
  def setChannelName(x: String): Unit = channel_name.set(x)
  def getChannelNameProperty: SimpleStringProperty = channel_name
  def getChannelAttenuation: Double = channel_attenuation.get()
  def setChannelAttenuation(x: Double): Unit = channel_attenuation.set(x)
  def getChannelAttenuationProperty: SimpleDoubleProperty = channel_attenuation
  def addInvalidationListener(l: InvalidationListener): Unit = invalidationListeners = invalidationListeners + l
  def getChannelLevelDb: Double = channel_level_db.get
  def setChannelLevelDb(x: Double): Unit = channel_level_db.set(x)
  def getChannelLevelDbProperty: SimpleDoubleProperty = channel_level_db


  val superInvalidate = new InvalidationListener {
    override def invalidated(observable: Observable) = {
      invalidationListeners.foreach(_.invalidated(observable))
    }
  }

  channel_attenuation.addListener(superInvalidate)

  def handleMixOutput(channelLevel: Map[String, Float]): Unit = {
    setChannelLevelDb(channelLevel.getOrElse(id, Float.NegativeInfinity).toDouble)
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case x: BusChannelModel => x.id == id
      case _ => false
    }
  }
}

class BusChannelController(model: BusChannelModel) {
  @FXML var bpane_mix_channel: BorderPane = _
  @FXML var label_gain: Label = _
  @FXML var label_channel_name: Label = _

  def initialize() = {
    val fader = new Fader()
    fader.getLevelDbProperty.bindBidirectional(model.getChannelLevelDbProperty)
    label_gain.textProperty().bind(fader.getAtenuationProperty.asString("%.1f"))
    bpane_mix_channel.setCenter(fader)

    model.getChannelAttenuationProperty.bindBidirectional(fader.getAtenuationProperty)

    label_channel_name.textProperty().bind(model.getChannelNameProperty)
  }
}
