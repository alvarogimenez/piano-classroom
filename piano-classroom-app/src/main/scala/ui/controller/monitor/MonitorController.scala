package ui.controller.monitor

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.BorderPane
import javafx.stage.{Stage, StageStyle}

import ui.controller.component.ScreenSelector

trait MonitorController {
  @FXML var bpane_monitor_screen: BorderPane = _
  @FXML var button_show_fullscreen: Button = _
  @FXML var button_hide_fullscreen: Button = _

  val screenSelector = new ScreenSelector()
  val screenStage = new Stage()
  screenStage.setScene(new Scene(new BorderPane()))
  screenStage.setTitle("Monitor")
  screenStage.initStyle(StageStyle.UNDECORATED)

  def initializeMonitorController() = {
    bpane_monitor_screen.setCenter(screenSelector)

    button_show_fullscreen.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        screenSelector.getSelectedScreen match {
          case Some(s) =>
            screenStage.setResizable(false)
            screenStage.setX(s.screen.getBounds.getMinX)
            screenStage.setY(s.screen.getBounds.getMinY)
            screenStage.setWidth(s.screen.getBounds.getWidth)
            screenStage.setHeight(s.screen.getBounds.getHeight)
            screenStage.show()
          case _ =>
        }
      }
    })

    button_hide_fullscreen.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        screenStage.hide()
      }
    })
  }
}
