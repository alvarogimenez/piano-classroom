package ui.controller.monitor

import javafx.beans.property.{SimpleIntegerProperty, SimpleObjectProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.{Button, Toggle, ToggleButton, ToggleGroup}
import javafx.scene.image.ImageView
import javafx.scene.layout.{BorderPane, StackPane}
import javafx.scene.shape.Rectangle
import javafx.stage.{Stage, StageStyle}

import context.Context
import io.contracts.{GlobalConfiguration, GlobalMonitorCameraSettings, GlobalMonitorConfiguration, GlobalMonitorDrawBoardSettings}
import ui.controller.component.ScreenSelector
import ui.controller.monitor.MonitorSource.MonitorSource

class MonitorModel {
  val monitorWebCamModel = new MonitorWebCamModel()
  val monitorDrawBoardModel = new MonitorDrawBoardModel()

  val selected_source_toggle: SimpleObjectProperty[Toggle] = new SimpleObjectProperty[Toggle]()
  val selected_target_monitor: SimpleIntegerProperty = new SimpleIntegerProperty()

  def getSelectedSource: Toggle = selected_source_toggle.get()
  def setSelectedSourceToggle(t: Toggle):Unit = selected_source_toggle.set(t)
  def getSelectedSourceToggleProperty: SimpleObjectProperty[Toggle] = selected_source_toggle

  def getSelectedTargetMonitor: Int = selected_target_monitor.get
  def setSelectedTargetMonitor(t: Int): Unit = selected_target_monitor.set(t)
  def getSelectedTargetMonitorProperty: SimpleIntegerProperty = selected_target_monitor
}

trait MonitorController {
  @FXML var bpane_monitor_screen: BorderPane = _
  @FXML var bpane_monitor: BorderPane = _
  @FXML var button_show_fullscreen: Button = _
  @FXML var button_hide_fullscreen: Button = _
  @FXML var toggle_monitor_source: ToggleGroup = _
  @FXML var toggle_camera: ToggleButton = _
  @FXML var toggle_pencil: ToggleButton = _
  @FXML var toggle_board: ToggleButton = _
  @FXML var toggle_music: ToggleButton = _

  private val screenSelector = new ScreenSelector()
  private val screenStage = new Stage()
  private val screenImage = new ImageView()
  private val screenCanvas = new Canvas()
  private val screenPane = new StackPane()

  val monitorWebCamController = new MonitorWebCamController(this, Context.monitorModel.monitorWebCamModel)
  val monitorDrawBoardController = new MonitorDrawBoardController(Context.monitorModel.monitorDrawBoardModel)
  private val monitorWebCamView = loadWebCamView()
  private val monitorDrawBoardView = loadDrawBoardView()

  def initializeMonitorController() = {
    toggle_camera.setUserData(MonitorSource.CAMERA)
    toggle_pencil.setUserData(MonitorSource.PENCIL)
    toggle_board.setUserData(MonitorSource.BOARD)
    toggle_music.setUserData(MonitorSource.MUSIC)

    Context.monitorModel.getSelectedSourceToggleProperty.bind(toggle_monitor_source.selectedToggleProperty())

    screenImage.setPreserveRatio(true)
    screenImage.fitWidthProperty().bind(screenPane.widthProperty())
    screenCanvas.widthProperty().bind(screenPane.widthProperty())
    screenCanvas.heightProperty().bind(screenPane.heightProperty())
    screenPane.getChildren.add(screenImage)
    screenPane.getChildren.add(screenCanvas)
    screenStage.setScene(new Scene(screenPane))
    screenStage.setTitle("Monitor")
    screenStage.initStyle(StageStyle.UNDECORATED)


    bpane_monitor_screen.setCenter(screenSelector)

    button_show_fullscreen.disableProperty().bind(screenSelector.getSelectedScreenProperty.isEqualTo(None))
    button_hide_fullscreen.disableProperty().bind(screenStage.showingProperty().not())

    screenSelector.getSelectedScreenProperty.addListener(new ChangeListener[Option[screenSelector.ScreenDefinition]] {
      override def changed(observable: ObservableValue[_ <: Option[screenSelector.ScreenDefinition]], oldValue: Option[screenSelector.ScreenDefinition], newValue: Option[screenSelector.ScreenDefinition]) = {
        newValue match {
          case Some(s) => Context.monitorModel.setSelectedTargetMonitor(s.index)
          case _ =>
        }
      }
    })

    button_show_fullscreen.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        goMonitorFullScreen()
        updateSession()
      }
    })

    button_hide_fullscreen.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        screenStage.hide()
        updateSession()
      }
    })

    val decoratorListener = new ChangeListener[GraphicsDecorator] {
      override def changed(observable: ObservableValue[_ <: GraphicsDecorator], oldValue: GraphicsDecorator, newValue: GraphicsDecorator): Unit = {
        val gc = screenCanvas.getGraphicsContext2D
        newValue.decorator(
          gc,
          new Rectangle(
            screenCanvas.getLayoutBounds.getMinX,
            screenCanvas.getLayoutBounds.getMinY,
            screenCanvas.getLayoutBounds.getWidth,
            screenCanvas.getLayoutBounds.getHeight
          )
        )
      }
    }

    Context.monitorModel.monitorWebCamModel.getDecoratorProperty.addListener(decoratorListener)
    Context.monitorModel.monitorDrawBoardModel.getDecoratorProperty.addListener(decoratorListener)

    Context.monitorModel.getSelectedSourceToggleProperty.addListener( new ChangeListener[Toggle] {
      override def changed(observable: ObservableValue[_ <: Toggle], oldValue: Toggle, newValue: Toggle): Unit = {
        screenImage.imageProperty().unbind()
        screenImage.setImage(null)
        monitorWebCamController.stop()
        monitorDrawBoardController.stop()

        if(newValue != null) {
          newValue.getUserData match {
            case MonitorSource.CAMERA =>
              bpane_monitor.setCenter(monitorWebCamView)
              screenImage.imageProperty().bind(Context.monitorModel.monitorWebCamModel.getSourceImageProperty)
              //            Context.trackSetModel.getTrackSet.headOption.map(_.addTrackSubscriber(monitorWebCamController))
              monitorWebCamController.start()
            case MonitorSource.PENCIL =>
              bpane_monitor.setCenter(monitorDrawBoardView)
              monitorDrawBoardController.start()
          }
        }

        updateSession()
      }
    })
  }

  private def loadWebCamView() = {
    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/MonitorWebCamView.fxml"))
    loader.setController(monitorWebCamController)
    loader.load.asInstanceOf[BorderPane]
  }

  private def loadDrawBoardView() = {
    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/MonitorDrawBoardView.fxml"))
    loader.setController(monitorDrawBoardController)
    loader.load.asInstanceOf[BorderPane]
  }

  def updateSession(): Unit = {
    val monitorConfiguration =
      GlobalMonitorConfiguration(
        `source-index` = Context.monitorModel.getSelectedTargetMonitor,
        `fullscreen` = screenStage.isShowing,
        `active-view` = Option(Context.monitorModel.getSelectedSource).map(_.getUserData.toString),
        `camera-settings` = GlobalMonitorCameraSettings(
          `source` = Option(Context.monitorModel.monitorWebCamModel.getSelectedSource).map(_.name)
        ),
        `draw-board-settings` = GlobalMonitorDrawBoardSettings()
      )

    context.writeSessionSettings(
      Context.sessionSettings.copy(
        `global` =
          Some(
            Context.sessionSettings.`global`
              .getOrElse(GlobalConfiguration())
              .copy(`monitor` = Some(monitorConfiguration))
          )
      )
    )
  }

  def selectMonitorView(view: MonitorSource): Unit = {
    getMonitorAvailableSourceToggles
      .find(_.getUserData == view)
      .foreach { source =>
        source.setSelected(true)
      }
  }

  def getMonitorAvailableSourceToggles: List[Toggle] = {
    List(toggle_camera, toggle_board, toggle_music, toggle_pencil)
  }

  def selectMonitorSourceWithIndex(index: Int): Unit = {
    screenSelector.setSelectedScreenByIndex(index)
  }

  def goMonitorFullScreen(): Unit = {
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
}
