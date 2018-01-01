package ui.controller.mixer

import javafx.beans.property.{SimpleDoubleProperty, SimpleListProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control._
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}

import sound.audio.mixer.{BusMix, ChannelMix}
import ui.controller.component.{CompressorPreview, Fader, ProfileButton}
import ui.controller.track.TrackModel

import scala.collection.JavaConversions._

class BusMixModel(val channel: Int) {
  var invalidationListeners: Set[InvalidationListener] = Set.empty[InvalidationListener]
  val bus_channels_ol: ObservableList[BusChannelModel] = FXCollections.observableArrayList[BusChannelModel]
  val bus_channels: SimpleListProperty[BusChannelModel] = new SimpleListProperty[BusChannelModel](bus_channels_ol)
  val bus_level_db: SimpleDoubleProperty = new SimpleDoubleProperty()
  val bus_attenuation: SimpleDoubleProperty = new SimpleDoubleProperty()

  def getBusChannels: List[BusChannelModel] = bus_channels_ol.toList
  def setBusChannels(l: List[BusChannelModel]): Unit = bus_channels.setAll(l)
  def addBusChannel(m: BusChannelModel): Unit = bus_channels_ol.add(m)
  def removeBusChannel(m: BusChannelModel): Unit = bus_channels_ol.remove(m)
  def getBusChannelsProperty: SimpleListProperty[BusChannelModel] = bus_channels
  def addInvalidationListener(l: InvalidationListener): Unit = invalidationListeners = invalidationListeners + l
  def getBusLevelDb: Double = bus_level_db.get
  def setBusLevelDb(x: Double): Unit = bus_level_db.set(x)
  def getBusLevelDbProperty: SimpleDoubleProperty = bus_level_db
  def getBusAttenuation: Double = bus_attenuation.get
  def setBusAttenuation(x: Double): Unit = bus_attenuation.set(x)
  def getBusAttenuationProperty: SimpleDoubleProperty = bus_attenuation

  val superInvalidate = new InvalidationListener {
    override def invalidated(observable: Observable) = {
      invalidationListeners.foreach(_.invalidated(observable))
    }
  }

  bus_channels.addListener(superInvalidate)
  bus_channels.addListener(new InvalidationListener {
    override def invalidated(observable: Observable) = {
      getBusChannels.foreach(_.addInvalidationListener(superInvalidate))
    }
  })

  def handleMixOutput(channelLevel: Map[String, Float], busLevel: Map[Int, Float]): Unit = {
    setBusLevelDb(busLevel.getOrElse(channel, Float.NegativeInfinity).toDouble)
    getBusChannels.foreach(_.handleMixOutput(channelLevel))
  }

  def dumpMix: BusMix = {
    BusMix(
      bus = channel,
      level = Math.pow(10, getBusAttenuation/20.0).toFloat,
      channelMix = getBusChannels
        .map { busChannel =>
          ChannelMix(busChannel.id, Math.pow(10, busChannel.getChannelAttenuation/20.0).toFloat)
        }
    )    
  }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case x: BusMixModel => x.channel == channel
      case _ => false
    }
  }
}

class BusMixController(parentController: MixerController, model: BusMixModel) {
  @FXML var hbox_bus_profiles: HBox = _
  @FXML var scrollpane_bus_profiles: ScrollPane = _
  
  @FXML var vbox_bus_faders: VBox = _
  @FXML var bpane_gain_fader: BorderPane = _
  @FXML var bpane_compressor_preview: BorderPane = _

  @FXML var label_master_gain: Label = _

  def initialize() = {
    List("Only Master", "All", "Master + Individual C1", "Master + Individual C2", "Master + Individual C3", "Master + Individual C4").foreach { i =>
      val b = new ProfileButton(i)
      b.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
        override def handle(event: MouseEvent): Unit = {
          scrollpane_bus_profiles.fireEvent(event)
          hbox_bus_profiles.fireEvent(event)
        }
      })
      b.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent) = {
          println(s"Button $i pressed")
        }
      })
      hbox_bus_profiles.getChildren.add(b)
    }

    val fader = new Fader()
    model.setBusLevelDb(Double.MinValue)

    fader.getLevelDbProperty.bindBidirectional(model.getBusLevelDbProperty)
    label_master_gain.textProperty().bind(fader.getAtenuationProperty.asString("%.1f"))
    bpane_gain_fader.setCenter(fader)

    fader.getAtenuationProperty.bindBidirectional(model.getBusAttenuationProperty)
    model.getBusAttenuationProperty.addListener(new ChangeListener[Number]{
      override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
        parentController.updateMixerSession()
      }
    })

    val compressorPreview = new CompressorPreview()
    bpane_compressor_preview.setCenter(compressorPreview)

    model.getBusChannelsProperty.addListener(new ListChangeListener[BusChannelModel] {
      override def onChanged(c: Change[_ <: BusChannelModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { busChannelModel =>
                val loader = new FXMLLoader()
                loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/BusChannelMixPanel.fxml"))
                loader.setController(new BusChannelController(parentController, busChannelModel))
                val channel = loader.load().asInstanceOf[BorderPane]
                channel.setUserData(busChannelModel)
                vbox_bus_faders.getChildren.add(channel)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { busChannelModel =>
                vbox_bus_faders.getChildren.remove(vbox_bus_faders.getChildren.find(_.getUserData == busChannelModel))
              }
          }
        }
        parentController.updateMixerSession()
      }
    })
  }
}
