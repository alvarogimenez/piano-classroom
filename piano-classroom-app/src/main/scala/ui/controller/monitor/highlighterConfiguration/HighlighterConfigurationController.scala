package ui.controller.monitor.highlighterConfiguration

import java.awt.image.BufferedImage
import java.util.concurrent.Callable
import javafx.beans.binding.Bindings
import javafx.beans.property.{SimpleObjectProperty, _}
import javafx.embed.swing.SwingFXUtils
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage

import com.sksamuel.scrimage.filter.ThresholdFilter
import com.sksamuel.scrimage.{Image => ScrImage}
import ui.controller.component.SliderStack
import ui.controller.component.SliderStack.SliderType
import util.KeyboardLayoutUtils
import util.KeyboardLayoutUtils.KeyboardLayout

import scala.util.Try

class HighlighterConfigurationModel {
  var exit_status: Int = _
  var image: ObjectProperty[BufferedImage] = new SimpleObjectProperty[BufferedImage]()
  var brightness_threshold: ObjectProperty[Integer]  = new SimpleObjectProperty[Integer]()
  var smooth_average: ObjectProperty[Integer]  = new SimpleObjectProperty[Integer]()
  var keyboard_layout: ObjectProperty[KeyboardLayout] = new SimpleObjectProperty[KeyboardLayout]()
  var preview_brightness_threshold: BooleanProperty = new SimpleBooleanProperty()
  var preview_keyboard_layout: BooleanProperty = new SimpleBooleanProperty()
  
  def getExitStatus: Int = exit_status
  def setExitStatus(s: Int): Unit = exit_status = s

  def getImage: BufferedImage = image.get()
  def setImage(i: BufferedImage): Unit = image.set(i)
  def getImageProperty: ObjectProperty[BufferedImage] = image

  def getBrightnessThreshold: Int = brightness_threshold.get
  def setBrightnessThreshold(b: Int): Unit = brightness_threshold.set(b)
  def getBrightnessThresholdProperty: ObjectProperty[Integer] = brightness_threshold

  def getSmoothAverage: Int = smooth_average.get
  def setSmoothAverage(b: Int): Unit = smooth_average.set(b)
  def getSmoothAverageProperty: ObjectProperty[Integer] = smooth_average

  def getKeyboardLayout: KeyboardLayout = keyboard_layout.get
  def setKeyboardLayout(b: KeyboardLayout): Unit = keyboard_layout.set(b)
  def getKeyboardLayoutProperty: ObjectProperty[KeyboardLayout] = keyboard_layout

  def getPreviewBrightnessThreshold: Boolean = preview_brightness_threshold.get
  def setPreviewBrightnessThreshold(b: Boolean): Unit = preview_brightness_threshold.set(b)
  def getPreviewBrightnessThresholdProperty: BooleanProperty = preview_brightness_threshold

  def getPreviewKeyboardLayout: Boolean = preview_keyboard_layout.get
  def setPreviewKeyboardLayout(b: Boolean): Unit = preview_keyboard_layout.set(b)
  def getPreviewKeyboardLayoutProperty: BooleanProperty = preview_keyboard_layout

  
  setBrightnessThreshold(25)
  setSmoothAverage(1)
  setPreviewBrightnessThreshold(true)
  setPreviewKeyboardLayout(true)
}

class HighlighterConfigurationController(dialog: Stage, model: HighlighterConfigurationModel) {
  @FXML var button_cancel: Button = _
  @FXML var button_accept: Button = _
  @FXML var imageview_camera: ImageView = _
  @FXML var stack_image_controls: StackPane = _
  @FXML var spinner_brightness_threshold: Spinner[Integer] = _
  @FXML var spinner_smooth_average: Spinner[Integer] = _
  @FXML var button_calculate: Button = _
  @FXML var checkbox_preview_brightness_threshold: CheckBox = _ 
  @FXML var checkbox_preview_keyboard_layout: CheckBox = _ 
  @FXML var label_calculate_error: Label = _

  val cutYSlider = new SliderStack(SliderType.HORIZONTAL, Color.RED, "Cut(Y)")

  def initialize() : Unit = {
    imageview_camera.imageProperty().bind(Bindings.createObjectBinding[Image](
      new Callable[Image] {
        override def call(): Image = {
          val modifiers: List[(Boolean, (ScrImage) => ScrImage)] =
            List(
              (
                model.getPreviewBrightnessThreshold,
                (i: ScrImage) => {
                  i.filter(ThresholdFilter(Math.min(255, model.getBrightnessThreshold / 100.0 * 255).toInt))
                }
              ),
              (
                model.getKeyboardLayout != null && model.getPreviewKeyboardLayout,
                (i: ScrImage) => {
                  KeyboardLayoutUtils.paintFullLayout(model.getImage, i.awt, model.getKeyboardLayout)
                  i
                }
              )
            )

          SwingFXUtils.toFXImage(
            modifiers
              .foldLeft(ScrImage.fromAwt(model.getImage)) {
                case (img, (true, mod)) => mod(img)
                case (img, (false, _)) => img
              }
              .awt, null)
        }
      },
      model.getImageProperty,
      model.getPreviewBrightnessThresholdProperty,
      model.getBrightnessThresholdProperty,
      model.getPreviewKeyboardLayoutProperty,
      model.getKeyboardLayoutProperty)
    )

    stack_image_controls.getChildren.add(cutYSlider)

    val brightnessThresholdFactory = new IntegerSpinnerValueFactory(0, 100)
    spinner_brightness_threshold.setValueFactory(brightnessThresholdFactory)
    spinner_brightness_threshold.getValueFactory.valueProperty().bindBidirectional(model.getBrightnessThresholdProperty)

    val smoothAverageFactory = new IntegerSpinnerValueFactory(0, 100)
    spinner_smooth_average.setValueFactory(smoothAverageFactory)
    spinner_smooth_average.getValueFactory.valueProperty().bindBidirectional(model.getBrightnessThresholdProperty)

    checkbox_preview_brightness_threshold.selectedProperty().bindBidirectional(model.getPreviewBrightnessThresholdProperty)
    checkbox_preview_keyboard_layout.selectedProperty().bindBidirectional(model.getPreviewKeyboardLayoutProperty)

    button_calculate.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        try {
          val layout =
          KeyboardLayoutUtils.extractLayoutFromImage(
            model.getImage,
            model.getBrightnessThreshold / 100.0,
            model.getSmoothAverage / 100.0,
            cutYSlider.position.get()
          )
          model.setKeyboardLayout(layout)
          label_calculate_error.setText("")
          label_calculate_error.setVisible(false)
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

}
