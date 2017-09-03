package ui.controller.mixer

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, ScrollPane}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}


class BusMixController {
  @FXML var hbox_bus_profiles: HBox = _
  @FXML var scrollpane_bus_profiles: ScrollPane = _
  
  @FXML var vbox_bus_faders: VBox = _
  @FXML var bpane_master: BorderPane = _

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

    bpane_master.setCenter(new Fader())
  }
}
