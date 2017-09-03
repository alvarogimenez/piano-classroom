package ui.controller.mixer

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.{Button, ScrollPane, Tab, TabPane}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.paint.Color

import ui.controller.settings.SettingsController

case class MixerGlobalProfile(
  name: String,
  color: Color,
  busMixes: List[MixerBusMixProfile]
)

case class MixerBusProfile(
  bus: Int,
  name: String,
  color: Color,
  mixes: MixerBusMixProfile
)

case class MixerBusMixProfile(
  bus: Int,
  mixes: List[MixerChannelMixProfile]
)

case class MixerChannelMixProfile(
  channel: String,
  mix: Float,
  active: Boolean,
  solo: Boolean
)

class MixerModel {

}

trait MixerController {
  @FXML var hbox_mixer_profiles: HBox = _
  @FXML var scrollpane_mixer_profiles: ScrollPane = _
  @FXML var tabs_bus_mixes: TabPane = _

  def initializeMixerController() = {
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

    val loader = new FXMLLoader()
    loader.setController(this)
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/BusPanel.fxml"))
    loader.setController(new BusMixController())

    val tab = new Tab()
    tab.setContent(loader.load().asInstanceOf[BorderPane])
    tab.setText("Test")
    tabs_bus_mixes.getTabs.clear()
    tabs_bus_mixes.getTabs.add(tab)
  }
}
