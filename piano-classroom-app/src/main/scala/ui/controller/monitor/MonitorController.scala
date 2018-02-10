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
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.{Stage, StageStyle}

import context.Context
import io.contracts._
import ui.controller.MainStageController
import ui.controller.component.ScreenSelector
import ui.controller.global.ProjectSessionUpdating
import ui.controller.monitor.MonitorSource.MonitorSource
import ui.controller.monitor.drawboard.{CanvasLine, MonitorDrawBoardController, MonitorDrawBoardModel}
import ui.controller.monitor.webcam.{MonitorWebCamController, MonitorWebCamModel}

class MonitorModel {
  val monitorWebCamModel = new MonitorWebCamModel()
  val monitorDrawBoardModel = new MonitorDrawBoardModel()

  val selected_source_toggle: SimpleObjectProperty[Toggle] = new SimpleObjectProperty[Toggle]()
  val selected_target_monitor: SimpleIntegerProperty = new SimpleIntegerProperty()
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()

  def getSelectedSource: Toggle = selected_source_toggle.get()
  def setSelectedSourceToggle(t: Toggle):Unit = selected_source_toggle.set(t)
  def getSelectedSourceToggleProperty: SimpleObjectProperty[Toggle] = selected_source_toggle

  def getSelectedTargetMonitor: Int = selected_target_monitor.get
  def setSelectedTargetMonitor(t: Int): Unit = selected_target_monitor.set(t)
  def getSelectedTargetMonitorProperty: SimpleIntegerProperty = selected_target_monitor

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator
}

trait MonitorController {  _ : ProjectSessionUpdating =>
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
  val monitorDrawBoardController = new MonitorDrawBoardController(this, Context.monitorModel.monitorDrawBoardModel)
  private val monitorWebCamView = loadWebCamView()
  private val monitorDrawBoardView = loadDrawBoardView()

  def initializeMonitorController(mainController: MainStageController) = {
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
        updateProjectSession()
      }
    })

    button_hide_fullscreen.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        screenStage.hide()
        updateProjectSession()
      }
    })

    val decoratorListener = new ChangeListener[GraphicsDecorator] {
      override def changed(observable: ObservableValue[_ <: GraphicsDecorator], oldValue: GraphicsDecorator, newValue: GraphicsDecorator): Unit = {
        val gc = screenCanvas.getGraphicsContext2D
        if(newValue != null) {
          newValue.decorator(
            gc,
            new Rectangle(
              screenCanvas.getLayoutBounds.getMinX,
              screenCanvas.getLayoutBounds.getMinY,
              screenCanvas.getLayoutBounds.getWidth,
              screenCanvas.getLayoutBounds.getHeight
            )
          )
        } else {
          gc.setFill(Color.WHITE)
          gc.fillRect(
            screenCanvas.getLayoutBounds.getMinX,
            screenCanvas.getLayoutBounds.getMinY,
            screenCanvas.getLayoutBounds.getWidth,
            screenCanvas.getLayoutBounds.getHeight
          )
        }
      }
    }

    Context.monitorModel.getDecoratorProperty.addListener(decoratorListener)

    Context.monitorModel.getSelectedSourceToggleProperty.addListener( new ChangeListener[Toggle] {
      override def changed(observable: ObservableValue[_ <: Toggle], oldValue: Toggle, newValue: Toggle): Unit = {
        screenImage.imageProperty().unbind()
        Context.monitorModel.getDecoratorProperty.unbind()
        screenImage.setImage(null)
        monitorWebCamController.stop()
        monitorDrawBoardController.stop()

        if(newValue != null) {
          newValue.getUserData match {
            case MonitorSource.CAMERA =>
              bpane_monitor.setCenter(monitorWebCamView)
              screenImage.imageProperty().bind(Context.monitorModel.monitorWebCamModel.getSourceImageProperty)
              Context.monitorModel.getDecoratorProperty.bind(Context.monitorModel.monitorWebCamModel.getDecoratorProperty)
              monitorWebCamController.start()
            case MonitorSource.PENCIL =>
              bpane_monitor.setCenter(monitorDrawBoardView)
              Context.monitorModel.getDecoratorProperty.bind(Context.monitorModel.monitorDrawBoardModel.getDecoratorProperty)
              monitorDrawBoardController.start()
          }
        }

        updateProjectSession()
      }
    })
  }

  private def loadWebCamView() = {
    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/monitor/MonitorWebcamView.fxml"))
    loader.setController(monitorWebCamController)
    loader.load.asInstanceOf[BorderPane]
  }

  private def loadDrawBoardView() = {
    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/monitor/MonitorDrawBoardView.fxml"))
    loader.setController(monitorDrawBoardController)
    loader.load.asInstanceOf[BorderPane]
  }

  def getMonitorSession(): GlobalMonitorConfiguration =
      GlobalMonitorConfiguration(
        `source-index` = Context.monitorModel.getSelectedTargetMonitor,
        `fullscreen` = screenStage.isShowing,
        `active-view` = Option(Context.monitorModel.getSelectedSource).map(_.getUserData.toString),
        `camera-settings` = GlobalMonitorCameraSettings(
          `source` = Option(Context.monitorModel.monitorWebCamModel.getSelectedSource).map(_.name),
          `note-display`= Some(GlobalMonitorCameraNoteDisplaySettings(
            `source-track-id` = Option(Context.monitorModel.monitorWebCamModel.getTrackNoteSelectedSource).map(_.id),
            `display` =
              if(Context.monitorModel.monitorWebCamModel.isDisplayNoteInEnglish) {
                "English"
              } else if(Context.monitorModel.monitorWebCamModel.isDisplayNoteInFixedDo) {
                "FixedDo"
              } else {
                "NoDisplay"
              }
          )),
          `sustain-active`= Some(Context.monitorModel.monitorWebCamModel.isSustainActive),
          `highlighter-enabled` = Some(Context.monitorModel.monitorWebCamModel.isHighlightEnabled),
          `highlighter-subtractive` = Some(Context.monitorModel.monitorWebCamModel.isHighlightSubtractive),
          `highlighter-subtractive-sensibility` = Some(Context.monitorModel.monitorWebCamModel.getHighlightSubtractiveSensibility),
          `keyboard-layout`= Option(Context.monitorModel.monitorWebCamModel.getKeyboardLayout).map { kl =>
            GlobalMonitorKeyboardLayout(
            `layout-data` = kl.layout.map { k =>
              GlobalMonitorKeyboardLayoutData(
                `note`= k.key.note.toString,
                `note-index`=k.key.index,
                `left`= k.left,
                `right`= k.right,
                `top`= k.top,
                `bottom`= k.bottom,
                `mask`= k.mask.map { m =>
                  m.map(_.mkString(";")).mkString("|")
                }
              )
            },
            `brightness-threshold` = kl.brightnessThreshold,
            `smooth-average`= kl.smoothAverage,
            `cut-y`= kl.cutY
            )
          }
        ),
        `draw-board-settings` = GlobalMonitorDrawBoardSettings(
          `pens` = Some(
            Context
              .monitorModel
              .monitorDrawBoardModel
              .getAvailableColorButtons
                .map { cb =>
                  GlobalMonitorDrawBoardSettingsPen(
                    cb.millis * 1000,
                    (cb.color.getRed* 255).toInt,
                    (cb.color.getGreen* 255).toInt,
                    (cb.color.getBlue* 255).toInt
                  )
                }
          ),
          `selected-canvas-name` = Option(Context
              .monitorModel
              .monitorDrawBoardModel
              .getSelectedDrawBoardCanvasModel).map(_.getCanvasData.name),
          `canvas` = Some(
            Context
              .monitorModel
              .monitorDrawBoardModel
              .getDrawBoardCanvasModels
              .map { m =>
                GlobalMonitorDrawBoardSettingsCanvas(
                  m.getCanvasData.name,
                  m.getCanvasData.aspectRatio,
                  m.getCanvasData.shapes.map {
                    case x: CanvasLine =>
                      GlobalMonitorDrawBoardSettingsCanvasLine(
                         `id` = x.id,
                         `size` = x.size,
                         `color` = GlobalMonitorDrawBoardSettingsCanvasColor(
                           `r` = (x.color.getRed * 255).toInt,
                           `g` = (x.color.getGreen * 255).toInt,
                           `b` = (x.color.getBlue * 255).toInt
                         ),
                        `path` = CanvasLine.pathToString(x.path)
                      )
                  }.toList
                )
              }
          )
        )
      )

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
