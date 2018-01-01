package ui.controller.mixer

import javafx.beans.{InvalidationListener, Observable}
import javafx.beans.property.SimpleListProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, Event, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Group
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox}

import context.Context
import sound.audio.mixer.ChannelMix
import ui.controller.MainStageController
import ui.controller.component.ProfileButton
import ui.controller.track.TrackModel

import scala.collection.JavaConversions._

class MixerModel {
  var invalidationListeners: Set[InvalidationListener] = Set.empty[InvalidationListener]
  val bus_mixes_ol: ObservableList[BusMixModel] = FXCollections.observableArrayList[BusMixModel]
  val bus_mixes: SimpleListProperty[BusMixModel] = new SimpleListProperty[BusMixModel](bus_mixes_ol)

  def getBusMixes: List[BusMixModel] = bus_mixes_ol.toList
  def setBusMixes(l: List[BusMixModel]): Unit = bus_mixes.setAll(l)
  def addBusMix(m: BusMixModel): Unit = bus_mixes_ol.add(m)
  def removeBusMix(m: BusMixModel): Unit = bus_mixes_ol.remove(m)
  def clear(): Unit = bus_mixes_ol.clear()
  def getBusMixesProperty: SimpleListProperty[BusMixModel] = bus_mixes

  def addInvalidationListener(l: InvalidationListener): Unit = invalidationListeners = invalidationListeners + l

  def dumpMix: Map[Int, Set[ChannelMix]] = {
    getBusMixes
      .map { busMixModel =>
        busMixModel.channel -> busMixModel.dumpMix
      }
      .toMap
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
              Context.trackSetModel.getTrackSetProperty.addListener(new ListChangeListener[TrackModel] {
                override def onChanged(c: Change[_ <: TrackModel]) = {
                  while(c.next()) {
                    if (c.getAddedSize != 0) {
                      c.getAddedSubList
                        .foreach { trackModel =>
                          val busChannelModel = new BusChannelModel(trackModel.channel.id)
                          busChannelModel.getChannelNameProperty.bind(trackModel.getTrackNameProperty)
                          busMixModel.addBusChannel(busChannelModel)
                        }
                    } else if (c.getRemovedSize != 0) {
                      c.getRemoved
                        .foreach { trackModel =>
                          busMixModel.getBusChannels.find(_.id == trackModel.channel.id).foreach(busMixModel.removeBusChannel)
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

trait MixerController {
  @FXML var hbox_mixer_profiles: HBox = _
  @FXML var scrollpane_mixer_profiles: ScrollPane = _
  @FXML var tabs_bus_mixes: TabPane = _

  var tab_add_bus: Tab = _

  def initializeMixerController(mainController: MainStageController) = {
    List("Default", "Broadcast").foreach { i =>
      val b = new ProfileButton(i)
      b.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
        override def handle(event: MouseEvent): Unit = {
          scrollpane_mixer_profiles.fireEvent(event)
          hbox_mixer_profiles.fireEvent(event)
        }
      })
      b.setOnAction(new EventHandler[ActionEvent] {
        override def handle(event: ActionEvent) = {
          println(s"Button $i pressed")
        }
      })
      hbox_mixer_profiles.getChildren.add(b)
    }

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

          model.setBusChannels(Context.trackSetModel.getTrackSet.map { t =>
            val busChannelModel = new BusChannelModel(t.channel.id)
            busChannelModel.getChannelNameProperty.bind(t.getTrackNameProperty)
            busChannelModel
          })
        }
      }
    })

    Context.mixerModel.getBusMixesProperty.addListener(new ListChangeListener[BusMixModel] {
      override def onChanged(c: Change[_ <: BusMixModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { busMixModel =>
                val loader = new FXMLLoader()
                loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/BusPanel.fxml"))
                loader.setController(new BusMixController(busMixModel))

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
      }
    })
  }

  def lastUsefulTabIndex(): Int = {
    val firstNotAdd = tabs_bus_mixes.getTabs.filter(_ != tab_add_bus).lastOption
    firstNotAdd match {
      case Some(t) => tabs_bus_mixes.getTabs.indexOf(t) + 1
      case _ => 0
    }
  }
}
