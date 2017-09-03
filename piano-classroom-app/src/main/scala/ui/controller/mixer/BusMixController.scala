package ui.controller.mixer

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{Button, Label, ScrollPane}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}


class BusMixController {
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
    label_master_gain.textProperty().bind(fader.getAtenuationProperty.asString("%.1f"))
    bpane_gain_fader.setCenter(fader)

    val compressorPreview = new CompressorPreview()
    bpane_compressor_preview.setCenter(compressorPreview)

    List("Channel A", "Channel B", "Channel C", "Channel D")
      .foreach { c =>
        vbox_bus_faders.getChildren.add(loadChannelMix())
      }
  }

  def loadChannelMix() = {
    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/BusChannelMixPanel.fxml"))
    loader.setController(new BusChannelController())
    loader.load().asInstanceOf[BorderPane]
  }
}
