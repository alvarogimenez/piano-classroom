package ui.controller.mixer

import javafx.beans.property.{SimpleDoubleProperty, SimpleListProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control._
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.stage.{Modality, Stage}

import context.Context
import sound.audio.mixer.{BusMix, ChannelMix}
import ui.controller.component.{CompressorPreview, Fader, ProfileButton}
import ui.controller.global
import ui.controller.global.ProjectSessionUpdating

import scala.collection.JavaConversions._
import scala.util.Random

class BusMixModel(val channel: Int) {
  var invalidationListeners: Set[InvalidationListener] = Set.empty[InvalidationListener]
  val bus_channels_ol: ObservableList[BusChannelModel] = FXCollections.observableArrayList[BusChannelModel]
  val bus_channels: SimpleListProperty[BusChannelModel] = new SimpleListProperty[BusChannelModel](bus_channels_ol)
  val bus_level_db: SimpleDoubleProperty = new SimpleDoubleProperty()
  val bus_attenuation: SimpleDoubleProperty = new SimpleDoubleProperty()
  val bus_mix_profiles_ol: ObservableList[BusMixProfile] = FXCollections.observableArrayList[BusMixProfile]
  val bus_mix_profiles: SimpleListProperty[BusMixProfile] = new SimpleListProperty[BusMixProfile](bus_mix_profiles_ol)

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

  def getBusMixProfiles: List[BusMixProfile] = bus_mix_profiles_ol.toList
  def setBusMixProfiles(l: List[BusMixProfile]): Unit = bus_mix_profiles.setAll(l)
  def addBusMixProfiles(m: BusMixProfile): Unit = bus_mix_profiles_ol.add(m)
  def removeMixProfiles(m: BusMixProfile): Unit = bus_mix_profiles_ol.remove(m)
  def getBusMixProfilesProperty: SimpleListProperty[BusMixProfile] = bus_mix_profiles

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

class BusMixController(parentController: ProjectSessionUpdating, model: BusMixModel) {
  @FXML var hbox_bus_profiles: HBox = _
  @FXML var scrollpane_bus_profiles: ScrollPane = _
  
  @FXML var vbox_bus_faders: VBox = _
  @FXML var bpane_gain_fader: BorderPane = _
  @FXML var bpane_compressor_preview: BorderPane = _

  @FXML var label_master_gain: Label = _
  @FXML var button_add_profile: Button = _

  def initialize() = {
    model.getBusMixProfilesProperty.addListener(new ListChangeListener[BusMixProfile] {
      override def onChanged(c: Change[_ <: BusMixProfile]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { busMixProfile =>
                val b = new ProfileButton(busMixProfile.name, busMixProfile.color)
                b.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
                  override def handle(event: MouseEvent): Unit = {
                    scrollpane_bus_profiles.fireEvent(event)
                    hbox_bus_profiles.fireEvent(event)
                  }
                })
                b.setOnAction(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    applyProfile(busMixProfile)
                  }
                })
                b.setOnDelete(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    model.removeMixProfiles(busMixProfile)
                  }
                })
                b.setUserData(busMixProfile)
                hbox_bus_profiles.getChildren.add(b)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .foreach { busChannelModel =>
                hbox_bus_profiles.getChildren.find(_.getUserData == busChannelModel).foreach { c =>
                  hbox_bus_profiles.getChildren.remove(c)
                }
              }
          }
        }
        parentController.updateProjectSession()
      }
    })

    val fader = new Fader()
    model.setBusLevelDb(Double.MinValue)

    fader.getLevelDbProperty.bindBidirectional(model.getBusLevelDbProperty)
    label_master_gain.textProperty().bind(fader.getAtenuationProperty.asString("%.1f"))
    bpane_gain_fader.setCenter(fader)

    fader.getAtenuationProperty.bindBidirectional(model.getBusAttenuationProperty)
    model.getBusAttenuationProperty.addListener(new ChangeListener[Number]{
      override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
        parentController.updateProjectSession()
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
                loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/mixer/BusChannelMixPanel.fxml"))
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
        parentController.updateProjectSession()
      }
    })

    button_add_profile.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        import global.profileSave._

        val dialog = new Stage()
        val loader = new FXMLLoader()
        val m = new ProfileSaveModel()
        val controller = new ProfileSaveController(dialog, m)

        m.setProfileNames(model.getBusMixProfiles.map(_.name))

        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/ProfileSaveDialog.fxml"))
        loader.setController(controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Save Profile")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if (m.getExitStatus == PROFILE_SAVE_MODAL_ACCEPT) {
          def busMixProfileFromModel = {
            val r = new Random(m.getResultName.hashCode)
            BusMixProfile(
              name = m.getResultName,
              color = new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1).desaturate(),
              busLevel = model.getBusAttenuation.toFloat,
              busMixes = model.getBusChannels.map { busChannel =>
                // TODO Set active and solo values
                BusChannelMixProfile(
                  channel = busChannel.id,
                  mix = busChannel.getChannelAttenuation.toFloat,
                  active = true,
                  solo = false
                )
              }
            )
          }

          m.getResultAction match {
            case ProfileSaveAction.OVERRIDE =>
              model.getBusMixProfiles.find(_.name == m.getResultName).foreach { removeMixProfile =>
                model.removeMixProfiles(removeMixProfile)
              }
              model.addBusMixProfiles(busMixProfileFromModel)
            case ProfileSaveAction.NEW =>
              model.addBusMixProfiles(busMixProfileFromModel)
            case _ =>
          }
        }
      }
    })
  }

  def applyProfile(p: BusMixProfile): Unit = {
    model.setBusAttenuation(p.busLevel)
    p.busMixes.foreach { busMix =>
      model.getBusChannels.find(_.id == busMix.channel).foreach { busChannel =>
        busChannel.setChannelAttenuation(busMix.mix)
        //TODO Set active and solo values
      }
    }
  }
}
