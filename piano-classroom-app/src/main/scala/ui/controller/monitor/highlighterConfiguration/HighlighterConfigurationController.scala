package ui.controller.monitor.highlighterConfiguration

import java.awt.image.BufferedImage
import java.util.concurrent.Callable
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.{SimpleDoubleProperty, SimpleObjectProperty, _}
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage

import com.sksamuel.scrimage.filter.ThresholdFilter
import com.sksamuel.scrimage.{Image => ScrImage}
import ui.controller.component.monitor.SliderStack
import ui.controller.component.monitor.SliderStack.SliderType
import ui.controller.monitor.highlighterConfiguration.HighlighterPreviewType.HighlighterPreviewType
import util.KeyboardLayoutUtils
import util.KeyboardLayoutUtils.LayoutMode.LayoutMode
import util.KeyboardLayoutUtils.{KeyboardLayout, LayoutMode}

object HighlighterPreviewType extends Enumeration {
  type HighlighterPreviewType = Value
  val ORIGINAL_IMAGE, THRESHOLD_IMAGE, CALCULATED_LAYOUT = Value
}

class HighlighterConfigurationModel {
  var exit_status: Int = _
  var image: ObjectProperty[BufferedImage] = new SimpleObjectProperty[BufferedImage]()
  var preview: ObjectProperty[BufferedImage] = new SimpleObjectProperty[BufferedImage]()
  var brightness_threshold: DoubleProperty  = new SimpleDoubleProperty()
  var cut_y: IntegerProperty  = new SimpleIntegerProperty()
  var smooth_average: DoubleProperty  = new SimpleDoubleProperty()
  var keyboard_layout: ObjectProperty[KeyboardLayout] = new SimpleObjectProperty[KeyboardLayout]()
  var highlighter_preview_type : ObjectProperty[HighlighterPreviewType] = new SimpleObjectProperty[HighlighterPreviewType]()
  var highlighter_layout_mode : ObjectProperty[LayoutMode] = new SimpleObjectProperty[LayoutMode]()

  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  def getImage: BufferedImage = image.get()
  def setImage(i: BufferedImage): Unit = image.set(i)
  def getImageProperty: ObjectProperty[BufferedImage] = image

  def getPreview: BufferedImage = preview.get()
  def setPreview(i: BufferedImage): Unit = preview.set(i)
  def getPreviewProperty: ObjectProperty[BufferedImage] = preview

  def getBrightnessThreshold: Double = brightness_threshold.get
  def setBrightnessThreshold(b: Double): Unit = brightness_threshold.set(b)
  def getBrightnessThresholdProperty: DoubleProperty = brightness_threshold

  def getSmoothAverage: Double = smooth_average.get
  def setSmoothAverage(b: Double): Unit = smooth_average.set(b)
  def getSmoothAverageProperty: DoubleProperty = smooth_average

  def getCutY: Int = cut_y.get
  def setCutY(c: Int): Unit = cut_y.set(c)
  def getCutYProperty: IntegerProperty = cut_y

  def getKeyboardLayout: KeyboardLayout = keyboard_layout.get
  def setKeyboardLayout(b: KeyboardLayout): Unit = keyboard_layout.set(b)
  def getKeyboardLayoutProperty: ObjectProperty[KeyboardLayout] = keyboard_layout

  def getHighlighterPreviewType: HighlighterPreviewType = highlighter_preview_type.get
  def setHighlighterPreviewType(h: HighlighterPreviewType): Unit = highlighter_preview_type.set(h)
  def getHighlighterPreviewTypeProperty: ObjectProperty[HighlighterPreviewType] = highlighter_preview_type

  def getHighlighterLayoutMode: LayoutMode = highlighter_layout_mode.get
  def setHighlighterLayoutMode(h: LayoutMode): Unit = highlighter_layout_mode.set(h)
  def getHighlighterLayoutModeProperty: ObjectProperty[LayoutMode] = highlighter_layout_mode
  
  setBrightnessThreshold(25)
  setSmoothAverage(1)
}

class HighlighterConfigurationController(dialog: Stage, model: HighlighterConfigurationModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var imageview_camera: ImageView = _
  @FXML var stack_image_controls: StackPane = _
  @FXML var slider_brightness_threshold: Slider = _
  @FXML var slider_smooth_average: Slider = _
  @FXML var button_calculate: Button = _
  @FXML var label_calculate_error: Label = _
  @FXML var radiobutton_preview_original_image: RadioButton = _
  @FXML var radiobutton_preview_threshold_image: RadioButton = _
  @FXML var radiobutton_preview_calculated_layout: RadioButton = _
  @FXML var radiobutton_layout_subtractive: RadioButton = _
  @FXML var radiobutton_layout_full: RadioButton = _
  @FXML var visualization_group: ToggleGroup = _
  @FXML var layout_mode: ToggleGroup = _
  @FXML var button_refresh_image: Button = _
  @FXML var progressbar_refresh_image: ProgressBar = _

  val cutYSlider = new SliderStack(SliderType.HORIZONTAL, Color.RED, "Cut(Y)")

  def initialize() : Unit = {
    imageview_camera.imageProperty().bind(Bindings.createObjectBinding[Image](
      new Callable[Image] {
        override def call(): Image = {
          val modifiers: List[(Boolean, (ScrImage) => ScrImage)] =
            List(
              (
                model.getHighlighterPreviewType == HighlighterPreviewType.THRESHOLD_IMAGE,
                (i: ScrImage) => {
                  i.filter(ThresholdFilter(Math.min(255, model.getBrightnessThreshold / 100.0 * 255).toInt))
                }
              ),
              (
                model.getKeyboardLayout != null && model.getHighlighterPreviewType == HighlighterPreviewType.CALCULATED_LAYOUT,
                (i: ScrImage) => {
                  KeyboardLayoutUtils.paintFullLayout(
                    src = model.getImage,
                    dst = i.awt,
                    keyboarLayout = model.getKeyboardLayout,
                    mode = Option(model.getHighlighterLayoutMode).getOrElse(LayoutMode.FullLayout),
                    sensibility = 0.25
                  )
                  i
                }
              )
            )

          SwingFXUtils.toFXImage(
            modifiers
              .foldLeft(ScrImage.fromAwt(model.getPreview)) {
                case (img, (true, mod)) => mod(img)
                case (img, (false, _)) => img
              }
              .awt, null)
        }
      },
      model.getPreviewProperty,
      model.getBrightnessThresholdProperty,
      model.getKeyboardLayoutProperty,
      model.getHighlighterPreviewTypeProperty,
      model.getHighlighterLayoutModeProperty)
    )

    stack_image_controls.getChildren.add(cutYSlider)
    cutYSlider.position.bindBidirectional(model.getCutYProperty)

    slider_brightness_threshold.valueProperty().bindBidirectional(model.getBrightnessThresholdProperty)
    slider_smooth_average.valueProperty().bindBidirectional(model.getSmoothAverageProperty)

    radiobutton_preview_original_image.setUserData(HighlighterPreviewType.ORIGINAL_IMAGE)
    radiobutton_preview_threshold_image.setUserData(HighlighterPreviewType.THRESHOLD_IMAGE)
    radiobutton_preview_calculated_layout.setUserData(HighlighterPreviewType.CALCULATED_LAYOUT)
    model.getHighlighterPreviewTypeProperty.bind(
      Bindings.createObjectBinding[HighlighterPreviewType](
        new Callable[HighlighterPreviewType] {
          override def call(): HighlighterPreviewType = {
            if (visualization_group.getSelectedToggle != null) {
              visualization_group.getSelectedToggle.getUserData.asInstanceOf[HighlighterPreviewType]
            } else {
              null
            }
          }
        },
        visualization_group.selectedToggleProperty()
      )
    )

    radiobutton_layout_subtractive.setUserData(LayoutMode.Subtractive)
    radiobutton_layout_full.setUserData(LayoutMode.FullLayout)
    model.getHighlighterLayoutModeProperty.bind(
      Bindings.createObjectBinding[LayoutMode](
        new Callable[LayoutMode] {
          override def call(): LayoutMode = {
            if (layout_mode.getSelectedToggle != null) {
              layout_mode.getSelectedToggle.getUserData.asInstanceOf[LayoutMode]
            } else {
              null
            }
          }
        },
        layout_mode.selectedToggleProperty()
      )
    )
    radiobutton_layout_subtractive.setSelected(true)

    if(model.getKeyboardLayout != null) {
      radiobutton_preview_calculated_layout.setSelected(true)
    } else {
      radiobutton_preview_threshold_image.setSelected(true)
    }

    button_refresh_image.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val thread = new Thread(refreshImageTask(2000))
        thread.setDaemon(true)
        thread.start()
      }
    })

    button_calculate.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        try {
          val layout =
          KeyboardLayoutUtils.extractLayoutFromImage(
            model.getPreview,
            model.getBrightnessThreshold / 100.0,
            model.getSmoothAverage / 100.0,
            model.getCutY
          )
          model.setKeyboardLayout(layout)
          label_calculate_error.setText("")
          label_calculate_error.setVisible(false)
          radiobutton_preview_calculated_layout.setSelected(true)
        } catch {
          case e: Exception =>
            e.printStackTrace()
            label_calculate_error.setText(e.getMessage)
            label_calculate_error.setVisible(true)
        }
      }
    })

    button_cancel.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(HIGHLIGHTER_CONFIGURATION_MODAL_CANCEL)
        dialog.close()
      }
    })

    button_accept.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        model.setExitStatus(HIGHLIGHTER_CONFIGURATION_MODAL_ACCEPT)
        dialog.close()
      }
    })
  }

  def refreshImageTask(delayMillis: Int) =
    new Task[Unit]() {
      override def call(): Unit = {
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            button_refresh_image.setDisable(true)
            progressbar_refresh_image.setProgress(0)
          }
        })
        (0 until 100).foreach { i =>
          Platform.runLater(new Runnable() {
            def run(): Unit = {
              progressbar_refresh_image.setProgress(i/100.0)
            }
          })
          Thread.sleep(delayMillis/100)
        }
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            model.setPreview(model.getImage)
            button_refresh_image.setDisable(false)
            progressbar_refresh_image.setProgress(0)
          }
        })
      }
    }

}
