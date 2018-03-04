package ui.controller.mixer

import javafx.beans.property.SimpleListProperty
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, Event, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.paint.Color
import javafx.scene.{Group, Scene}
import javafx.stage.{Modality, Stage}

import context.Context
import io.contracts._
import sound.audio.channel.Channel
import sound.audio.mixer.BusMix
import ui.controller.component.ProfileButton
import ui.controller.global.ProjectSessionUpdating
import ui.controller.{MainStageController, global}

import scala.collection.JavaConversions._
import scala.util.Random

class MixerModel {
  var invalidationListeners: Set[InvalidationListener] = Set.empty[InvalidationListener]
  val bus_mixes_ol: ObservableList[BusMixModel] = FXCollections.observableArrayList[BusMixModel]
  val bus_mixes: SimpleListProperty[BusMixModel] = new SimpleListProperty[BusMixModel](bus_mixes_ol)
  val mixer_profiles_ol: ObservableList[MixerProfile] = FXCollections.observableArrayList[MixerProfile]
  val mixer_profiles: SimpleListProperty[MixerProfile] = new SimpleListProperty[MixerProfile](mixer_profiles_ol)
  
  def getBusMixes: List[BusMixModel] = bus_mixes_ol.toList
  def setBusMixes(l: List[BusMixModel]): Unit = bus_mixes.setAll(l)
  def addBusMix(m: BusMixModel): Unit = bus_mixes_ol.add(m)
  def removeBusMix(m: BusMixModel): Unit = bus_mixes_ol.remove(m)
  def clear(): Unit = bus_mixes_ol.clear()
  def getBusMixesProperty: SimpleListProperty[BusMixModel] = bus_mixes

  def addInvalidationListener(l: InvalidationListener): Unit = invalidationListeners = invalidationListeners + l

  def getMixerProfiles: List[MixerProfile] = mixer_profiles_ol.toList
  def setMixerProfiles(l: List[MixerProfile]): Unit = mixer_profiles.setAll(l)
  def addMixerProfile(m: MixerProfile): Unit = mixer_profiles_ol.add(m)
  def removeMixerProfile(m: MixerProfile): Unit = mixer_profiles_ol.remove(m)
  def getMixerProfilesProperty: SimpleListProperty[MixerProfile] = mixer_profiles
  
  def dumpMix: List[BusMix] = {
    getBusMixes.map(_.dumpMix)
  }

  def handleMixOutput(channelLevel: Map[String, Float], busLevel: Map[Int, Float]): Unit = {
    getBusMixes.foreach(_.handleMixOutput(channelLevel, busLevel))
  }

  val superInvalidate = new InvalidationListener {
    override def invalidated(observable: Observable) = {
      invalidationListeners.foreach(_.invalidated(observable))
    }
  }

  bus_mixes.addListener(superInvalidate)
  bus_mixes.addListener(new InvalidationListener {
    override def invalidated(observable: Observable) = {
      getBusMixes.foreach(_.addInvalidationListener(superInvalidate))
    }
  })

  bus_mixes.addListener(new ListChangeListener[BusMixModel] {
    override def onChanged(c: Change[_ <: BusMixModel]) = {
      while(c.next()) {
        if (c.getAddedSize != 0) {
          c.getAddedSubList
            .foreach { busMixModel =>
              // When a new channel is created or removed, notify the BusMixModel with the changes
              Context.channelService.getChannelsProperty.addListener(new ListChangeListener[Channel] {
                override def onChanged(c: Change[_ <: Channel]) = {
                  while(c.next()) {
                    if (c.getAddedSize != 0) {
                      c.getAddedSubList
                        .foreach { channel =>
                          val busChannelModel = new BusChannelModel(channel.getId)
                          busChannelModel.getChannelNameProperty.bind(channel.getNameProperty)
                          busMixModel.addBusChannel(busChannelModel)
                        }
                    } else if (c.getRemovedSize != 0) {
                      c.getRemoved
                        .foreach { channel =>
                          busMixModel.getBusChannels.find(_.id == channel.getId).foreach(busMixModel.removeBusChannel)
                        }
                    }
                  }
                }
              })
            }
        } else if (c.getRemovedSize != 0) {
          // TODO Remove listeners when bus mix model is removed
        }
      }
    }
  })
}

trait MixerController { _ : ProjectSessionUpdating =>
  @FXML var hbox_mixer_profiles: HBox = _
  @FXML var scrollpane_mixer_profiles: ScrollPane = _
  @FXML var tabs_bus_mixes: TabPane = _
  @FXML var button_add_mixer_profile: Button = _

  var tab_add_bus: Tab = _
  private val _self = this

  def initializeMixerController(mainController: MainStageController) = {
    tab_add_bus = new Tab()
    val group = new Group()
    val graphic = new ImageView("assets/icon/Add.png")
    group.getChildren.add(graphic)
    group.setPickOnBounds(true)
    tab_add_bus.setGraphic(group)
    tab_add_bus.setDisable(true)
    tab_add_bus.setClosable(false)
    tab_add_bus.getStyleClass.add("add-tab")
    tabs_bus_mixes.getTabs().add(tab_add_bus)

    group.setOnMousePressed(new EventHandler[MouseEvent] {
      override def handle(event: MouseEvent) = {
        val availableChannels = (0 to 20).toList
        val dialog = new ChoiceDialog[Int](availableChannels.head, availableChannels)
        dialog.setTitle("Select an Output")
        dialog.setHeaderText("Select an Output Bus")
        dialog.setContentText("Select the Bus Output number to create the mixer console")

        val result = dialog.showAndWait()
        if(result.isPresent) {
          val model = new BusMixModel(result.get())
          Context.mixerModel.addBusMix(model)

          model.setBusChannels(Context.channelService.getChannels.map { c =>
            val busChannelModel = new BusChannelModel(c.getId)
            busChannelModel.getChannelNameProperty.bind(c.getNameProperty)
            busChannelModel
          })
        }
      }
    })

    Context.mixerModel.getMixerProfilesProperty.addListener(new ListChangeListener[MixerProfile] {
      override def onChanged(c: Change[_ <: MixerProfile]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { mixerProfile =>
                val b = new ProfileButton(mixerProfile.name, mixerProfile.color)
                b.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
                  override def handle(event: MouseEvent): Unit = {
                    scrollpane_mixer_profiles.fireEvent(event)
                    hbox_mixer_profiles.fireEvent(event)
                  }
                })
                b.setOnAction(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    applyProfile(mixerProfile)
                  }
                })
                b.setOnDelete(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    Context.mixerModel.removeMixerProfile(mixerProfile)
                  }
                })
                b.setUserData(mixerProfile)
                hbox_mixer_profiles.getChildren.add(b)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .foreach { busChannelModel =>
                hbox_mixer_profiles.getChildren.find(_.getUserData == busChannelModel).foreach { c =>
                  hbox_mixer_profiles.getChildren.remove(c)
                }
              }
          }
        }
        updateProjectSession()
      }
    })

    Context.mixerModel.getBusMixesProperty.addListener(new ListChangeListener[BusMixModel] {
      override def onChanged(c: Change[_ <: BusMixModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { busMixModel =>
                val loader = new FXMLLoader()
                loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/mixer/BusPanel.fxml"))
                loader.setController(new BusMixController(_self, busMixModel))

                val tab = new Tab()
                tab.setContent(loader.load().asInstanceOf[BorderPane])
                tab.setText(s"Bus ${busMixModel.channel}")
                tab.setClosable(true)
                tab.setUserData(busMixModel)

                tab.setOnCloseRequest(new EventHandler[Event] {
                  override def handle(event: Event) = {
                    val alert = new Alert(AlertType.CONFIRMATION)
                    alert.setTitle("Confirm Tab Close")
                    alert.setHeaderText("Confirm Tab Closing Action")
                    alert.setContentText("Are you sure you want to close this tab?")

                    val result = alert.showAndWait()
                    if(result.get() == ButtonType.OK) {
                      Context.mixerModel.removeBusMix(busMixModel)
                    } else {
                      event.consume()
                    }
                  }
                })

                tabs_bus_mixes.getTabs.add(lastUsefulTabIndex(), tab)
                tabs_bus_mixes.getSelectionModel.select(tab)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { busMixModel =>
                tabs_bus_mixes.getTabs.find(_.getUserData == busMixModel) match {
                  case Some(tab) =>
                    tabs_bus_mixes.getTabs.remove(tab)
                  case _ =>
                }
              }
          }
        }

        updateProjectSession()
      }
    })

    button_add_mixer_profile.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        import global.profileSave._

        val dialog = new Stage()
        val loader = new FXMLLoader()
        val m = new ProfileSaveModel()
        val controller = new ProfileSaveController(dialog, m)

        m.setProfileNames(Context.mixerModel.getMixerProfiles.map(_.name))

        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/ProfileSaveDialog.fxml"))
        loader.setController(controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Save Profile")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if (m.getExitStatus == PROFILE_SAVE_MODAL_ACCEPT) {
          def mixerProfileFromModel = {
            val r = new Random(m.getResultName.hashCode)
            MixerProfile(
              name = m.getResultName,
              color = new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1).desaturate(),
              busMixes = Context.mixerModel.getBusMixes.map { busMixModel =>
                BusProfile(
                  bus = busMixModel.channel,
                  busLevel = busMixModel.getBusAttenuation.toFloat,
                  busMixes = busMixModel.getBusChannels.map { busChannel =>
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
            )
          }

          m.getResultAction match {
            case ProfileSaveAction.OVERRIDE =>
              Context.mixerModel.getMixerProfiles.find(_.name == m.getResultName).foreach { removeMixProfile =>
                Context.mixerModel.removeMixerProfile(removeMixProfile)
              }
              Context.mixerModel.addMixerProfile(mixerProfileFromModel)
            case ProfileSaveAction.NEW =>
              Context.mixerModel.addMixerProfile(mixerProfileFromModel)
            case _ =>
          }
        }
      }
    })
  }

  def applyProfile(p: MixerProfile) = {
    p.busMixes.foreach { busMixProfile =>
      Context.mixerModel.getBusMixes.find(_.channel == busMixProfile.bus).foreach { busMixModel =>
        busMixModel.setBusAttenuation(busMixProfile.busLevel)
        busMixProfile.busMixes.foreach { busChannelMix =>
          busMixModel.getBusChannels.find(_.id == busChannelMix.channel).foreach { busChannel =>
            busChannel.setChannelAttenuation(busChannelMix.mix)
            //TODO Set active and solo values
          }
        }
      }
    }
  }

  def getMixerSession(): SaveMixer = 
      SaveMixer(
        `bus-info` = Context.mixerModel.getBusMixes.map { busMix =>
          SaveBusInfo(
            `bus` = busMix.channel,
            `master-level` = busMix.getBusAttenuation,
            `bus-mix`= busMix.getBusChannels.map { busChannel =>
              SaveBusMix(
                `channel-id` = busChannel.id,
                `level`= Some(busChannel.getChannelAttenuation)
              )
            },
            `bus-profiles` = busMix.getBusMixProfiles.map { busMixProfile =>
              SaveBusMixProfile(
                `name` = busMixProfile.name,
                `color`= util.colorToWebHex(busMixProfile.color),
                `bus-level` = busMixProfile.busLevel,
                `bus-mixes` = busMixProfile.busMixes.map { busChannelMix =>
                  SaveBusChannelMixProfile(
                    `channel` = busChannelMix.channel,
                    `mix` = busChannelMix.mix,
                    `active` = busChannelMix.active,
                    `solo` = busChannelMix.solo
                  )
                }
              )
            }
          )
        },
        `mixer-profiles`= Context.mixerModel.getMixerProfiles.map { mixerProfile =>
          SaveMixerProfile(
            `name` = mixerProfile.name,
            `color`= util.colorToWebHex(mixerProfile.color),
            `bus-profiles` = mixerProfile.busMixes.map { bus =>
              SaveBusProfile(
                `bus` = bus.bus,
                `bus-level` = bus.busLevel,
                `bus-mixes` = bus.busMixes.map { busChannelMix =>
                  SaveBusChannelMixProfile(
                    `channel` = busChannelMix.channel,
                    `mix` = busChannelMix.mix,
                    `active` = busChannelMix.active,
                    `solo` = busChannelMix.solo
                  )
                }
              )
            }
          )
        }
      )

  def lastUsefulTabIndex(): Int = {
    val firstNotAdd = tabs_bus_mixes.getTabs.filter(_ != tab_add_bus).lastOption
    firstNotAdd match {
      case Some(t) => tabs_bus_mixes.getTabs.indexOf(t) + 1
      case _ => 0
    }
  }
}
