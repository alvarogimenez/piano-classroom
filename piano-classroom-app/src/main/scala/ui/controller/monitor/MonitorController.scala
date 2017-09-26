package ui.controller.monitor

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.control.{Button, ToggleButton}
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.stage.{Stage, StageStyle}

import ui.controller.component.ScreenSelector
import ui.controller.mixer.BusMixController

trait MonitorController {
  @FXML var bpane_monitor_screen: BorderPane = _
  @FXML var bpane_monitor: BorderPane = _
  @FXML var button_show_fullscreen: Button = _
  @FXML var button_hide_fullscreen: Button = _
  @FXML var toggle_camera: ToggleButton = _
  @FXML var toggle_pencil: ToggleButton = _
  @FXML var toggle_board: ToggleButton = _
  @FXML var toggle_music: ToggleButton = _

  val screenSelector = new ScreenSelector()
  val screenStage = new Stage()
  val screenImage = new ImageView()
  val screenPane = new BorderPane()

  def initializeMonitorController() = {
    screenImage.setPreserveRatio(true)
    screenImage.fitWidthProperty().bind(screenPane.widthProperty())
    screenPane.setCenter(screenImage)
    screenStage.setScene(new Scene(screenPane))
    screenStage.setTitle("Monitor")
    screenStage.initStyle(StageStyle.UNDECORATED)


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

    toggle_camera.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        val model = new MonitorWebCamModel()
        val controller = new MonitorWebCamController(model)
        val loader = new FXMLLoader()
        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/MonitorWebCamView.fxml"))
        loader.setController(controller)
        bpane_monitor.setCenter(loader.load.asInstanceOf[BorderPane])
        screenImage.imageProperty().bind(model.getSourceImageProperty)
      }
    })
  }
}
