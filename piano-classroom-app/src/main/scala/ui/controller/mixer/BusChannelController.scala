package ui.controller.mixer

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane


class BusChannelController {
  @FXML var bpane_mix_channel: BorderPane = _
  @FXML var label_gain: Label = _
  @FXML var label_channel_name: Label = _

  def initialize() = {
    val fader = new Fader()
    label_gain.textProperty().bind(fader.getAtenuationProperty.asString("%.1f"))
    bpane_mix_channel.setCenter(fader)
  }
}
