package ui.controller.monitor

import java.util.concurrent.atomic.AtomicReference
import javafx.application.Platform
import javafx.beans.property.{SimpleListProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.image.{Image, ImageView, WritableImage}

import com.github.sarxos.webcam.Webcam

import scala.collection.JavaConversions._

class MonitorWebCamModel {
  val sources_ol: ObservableList[WebCamSource] = FXCollections.observableArrayList[WebCamSource]
  val sources: SimpleListProperty[WebCamSource] = new SimpleListProperty[WebCamSource](sources_ol)
  val selected_source: SimpleObjectProperty[WebCamSource] = new SimpleObjectProperty[WebCamSource]()
  val source_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()

  def getSources: List[WebCamSource] = sources.get().toList
  def setSources(l: List[WebCamSource]) = sources_ol.setAll(l)
  def getSourcesProperty: SimpleListProperty[WebCamSource] = sources

  def getSelectedSource: WebCamSource = selected_source.get()
  def setSelectedSource(s: WebCamSource) = selected_source.set(s)
  def getSelectedSourceProperty: SimpleObjectProperty[WebCamSource] = selected_source

  def getSourceImage: Image = source_image.get()
  def setSourceImage(i: Image): Unit = source_image.set(i)
  def getSourceImageProperty: SimpleObjectProperty[Image] = source_image
}

class MonitorWebCamController(model: MonitorWebCamModel) {
  @FXML var imageview_webcam: ImageView = _
  @FXML var combobox_source: ComboBox[WebCamSource] = _

  var currentWebCamTask: Task[Unit] = _

  def initialize() = {
    println()
    imageview_webcam.setPreserveRatio(true)
    imageview_webcam.setFitHeight(300.0)
    imageview_webcam.imageProperty().bind(model.getSourceImageProperty)

    model.setSources(List(null) ++ Webcam.getWebcams.toList.zipWithIndex.map { case (w, index) => WebCamSource(w.getName, index) })
    combobox_source.itemsProperty().bindBidirectional(model.getSourcesProperty)
    combobox_source.valueProperty().bindBidirectional(model.getSelectedSourceProperty)

    combobox_source.valueProperty().addListener(new ChangeListener[WebCamSource]() {
      override def changed(observable: ObservableValue[_ <: WebCamSource], oldValue: WebCamSource, newValue: WebCamSource): Unit = {
        println(s"Webcam changed from $oldValue to $newValue")

        if(currentWebCamTask != null) {
          currentWebCamTask.cancel()
        }

        if(newValue != null) {
          currentWebCamTask = webCamTask(newValue.index)
          val thread = new Thread(currentWebCamTask)
          thread.setDaemon(true)
          thread.start()
        }
      }
    })

  }

  def webCamTask(index: Int) = new Task[Unit]() {
    override def call(): Unit = {
      val cam = Webcam.getWebcams.get(index)
      cam.open()

      val ref = new AtomicReference[WritableImage]()
      while(!isCancelled) {
        val img = cam.getImage
        if(img != null) {
          ref.set(SwingFXUtils.toFXImage(img, ref.get()))
          img.flush()

          Platform.runLater(new Runnable() {
            def run(): Unit = {
              model.setSourceImage(ref.get())
            }
          })
        }
      }

      cam.close()
      Platform.runLater(new Runnable() {
        def run(): Unit = {
          model.setSourceImage(null)
        }
      })
    }
  }
}
