package ui.controller.monitor.webcam

import java.lang.Boolean
import java.util.concurrent.atomic.AtomicReference
import javafx.application.Platform
import javafx.beans.{InvalidationListener, Observable}
import javafx.beans.property.{SimpleBooleanProperty, SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.concurrent.Task
import javafx.concurrent.Worker.State
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.VPos
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.{ComboBox, ToggleButton}
import javafx.scene.image.{Image, ImageView, WritableImage}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, TextAlignment}

import com.github.sarxos.webcam.Webcam
import com.sun.javafx.tk.Toolkit
import context.Context
import ui.controller.component.drawboard.CanvasPreview
import ui.controller.monitor.drawboard.DrawBoardCanvasModel
import ui.controller.monitor._
import ui.controller.track.TrackModel
import ui.controller.track.pianoRange.TrackSubscriber
import util.KeyboardNote
import util.MusicNote.MusicNote

import scala.collection.JavaConversions._

class MonitorWebCamModel {
  val sources_ol: ObservableList[WebCamSource] = FXCollections.observableArrayList[WebCamSource]
  val sources: SimpleListProperty[WebCamSource] = new SimpleListProperty[WebCamSource](sources_ol)
  val selected_source: SimpleObjectProperty[WebCamSource] = new SimpleObjectProperty[WebCamSource]()
  val track_note_sources_ol: ObservableList[ChannelSource] = FXCollections.observableArrayList[ChannelSource]
  val track_note_sources: SimpleListProperty[ChannelSource] = new SimpleListProperty[ChannelSource](track_note_sources_ol)
  val track_note_selected_source: SimpleObjectProperty[ChannelSource] = new SimpleObjectProperty[ChannelSource]()
  val source_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()
  val displayNoteDisabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val displayNoteInEnglish: SimpleBooleanProperty = new SimpleBooleanProperty()
  val displayNoteInFixedDo: SimpleBooleanProperty = new SimpleBooleanProperty()

  def getSources: List[WebCamSource] = sources.get().toList
  def setSources(l: List[WebCamSource]) = sources_ol.setAll(l)
  def getSourcesProperty: SimpleListProperty[WebCamSource] = sources

  def getSelectedSource: WebCamSource = selected_source.get()
  def setSelectedSource(s: WebCamSource) = selected_source.set(s)
  def getSelectedSourceProperty: SimpleObjectProperty[WebCamSource] = selected_source

  def getTrackNoteSources: List[ChannelSource] = track_note_sources.get().toList
  def setTrackNoteSources(l: List[ChannelSource]) = track_note_sources_ol.setAll(l)
  def getTrackNoteSourcesProperty: SimpleListProperty[ChannelSource] = track_note_sources

  def getTrackNoteSelectedSource: ChannelSource = track_note_selected_source.get()
  def setTrackNoteSelectedSource(s: ChannelSource) = track_note_selected_source.set(s)
  def getTrackNoteSelectedSourceProperty: SimpleObjectProperty[ChannelSource] = track_note_selected_source

  def getSourceImage: Image = source_image.get()
  def setSourceImage(i: Image): Unit = source_image.set(i)
  def getSourceImageProperty: SimpleObjectProperty[Image] = source_image

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator

  def isDisplayNoteDisabled: Boolean = displayNoteDisabled.get
  def setDisplayNoteDisabled(d: Boolean): Unit = displayNoteDisabled.set(d)
  def getDisplayNoteDisabledProperty: SimpleBooleanProperty = displayNoteDisabled
  
  def isDisplayNoteInEnglish: Boolean = displayNoteInEnglish.get
  def setDisplayNoteInEnglish(d: Boolean): Unit = displayNoteInEnglish.set(d)
  def getDisplayNoteInEnglishProperty: SimpleBooleanProperty = displayNoteInEnglish

  def isDisplayNoteInFixedDo: Boolean = displayNoteInFixedDo.get
  def setDisplayNoteInFixedDo(d: Boolean): Unit = displayNoteInFixedDo.set(d)
  def getDisplayNoteInFixedDoProperty: SimpleBooleanProperty = displayNoteInFixedDo

  setDisplayNoteDisabled(true)
  setTrackNoteSources(List(null))

  Context.trackSetModel.getTrackSetProperty.addListener(new ListChangeListener[TrackModel] {
    override def onChanged(c: Change[_ <: TrackModel]) = {
      while (c.next()) {
        if (c.getAddedSize != 0) {
          c.getAddedSubList
            .foreach { trackModel =>
              recreateSourcesFromModel()
              trackModel.getTrackNameProperty.addListener(new InvalidationListener {
                override def invalidated(observable: Observable) = {
                  recreateSourcesFromModel()
                }
              })
            }
        } else if (c.getRemovedSize != 0) {
          c.getRemoved
            .map { trackModel =>
              recreateSourcesFromModel()
            }
        }
      }
    }
  })

  def recreateSourcesFromModel() = {
    setTrackNoteSources(List(null) ++ Context.trackSetModel.getTrackSet.map(t => ChannelSource(t.getTrackName, t.channel.id)))
  }
}

class MonitorWebCamController(parentController: MonitorController, model: MonitorWebCamModel) extends TrackSubscriber {
  @FXML var stackpane: StackPane = _
  @FXML var imageview_webcam: ImageView = _
  @FXML var canvas_overlay: Canvas = _
  @FXML var combobox_source: ComboBox[WebCamSource] = _
  @FXML var combobox_note_helper_source: ComboBox[ChannelSource] = _

  @FXML var toggle_note_display_no_display: ToggleButton = _ 
  @FXML var toggle_note_display_english: ToggleButton = _ 
  @FXML var toggle_note_display_fixed_do: ToggleButton = _ 
  
  var currentWebCamTask: Task[Unit] = _
  var _self = this

  trait NoteStatus
  case object NoteActive extends NoteStatus
  case object NoteSustained extends NoteStatus

  var activeNotes: Map[KeyboardNote, NoteStatus] = Map.empty
  var sustainActive = false

  def initialize() = {
    imageview_webcam.setPreserveRatio(true)
    imageview_webcam.fitWidthProperty().bind(stackpane.widthProperty())
    canvas_overlay.widthProperty().bind(stackpane.widthProperty())
    canvas_overlay.heightProperty().bind(stackpane.heightProperty())

    toggle_note_display_no_display.selectedProperty.bindBidirectional(model.getDisplayNoteDisabledProperty)
    toggle_note_display_english.selectedProperty().bindBidirectional(model.getDisplayNoteInEnglishProperty)
    toggle_note_display_fixed_do.selectedProperty().bindBidirectional(model.getDisplayNoteInFixedDoProperty)

    model.getDisplayNoteDisabledProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateMonitorSession()
    })
    model.getDisplayNoteInEnglishProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateMonitorSession()
    })
    model.getDisplayNoteInFixedDoProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateMonitorSession()
    })

    imageview_webcam.imageProperty().bind(model.getSourceImageProperty)
    model.getDecoratorProperty.addListener(new ChangeListener[GraphicsDecorator] {
      override def changed(observable: ObservableValue[_ <: GraphicsDecorator], oldValue: GraphicsDecorator, newValue: GraphicsDecorator): Unit = {
        val gc = canvas_overlay.getGraphicsContext2D
        newValue.decorator(
          gc,
          new Rectangle(
            canvas_overlay.getLayoutBounds.getMinX,
            canvas_overlay.getLayoutBounds.getMinY,
            canvas_overlay.getLayoutBounds.getWidth,
            canvas_overlay.getLayoutBounds.getHeight
          )
        )
      }
    })

    model.setSources(List(null) ++ Webcam.getWebcams.toList.zipWithIndex.map { case (w, index) => WebCamSource(w.getName, index) })
    combobox_source.itemsProperty().bindBidirectional(model.getSourcesProperty)
    combobox_source.valueProperty().bindBidirectional(model.getSelectedSourceProperty)

    combobox_note_helper_source.itemsProperty().bindBidirectional(model.getTrackNoteSourcesProperty)
    combobox_note_helper_source.valueProperty().bindBidirectional(model.getTrackNoteSelectedSourceProperty)

    model.getSelectedSourceProperty.addListener(new ChangeListener[WebCamSource]() {
      override def changed(observable: ObservableValue[_ <: WebCamSource], oldValue: WebCamSource, newValue: WebCamSource): Unit = {
        println(s"Webcam changed from $oldValue to $newValue")

        if(Context.monitorModel.getSelectedSource != null && Context.monitorModel.getSelectedSource.getUserData == MonitorSource.CAMERA) {
          start()
        }

        parentController.updateMonitorSession()
      }
    })

    model.getTrackNoteSelectedSourceProperty.addListener(new ChangeListener[ChannelSource] {
      override def changed(observable: ObservableValue[_ <: ChannelSource], oldValue: ChannelSource, newValue: ChannelSource) = {
        Context.trackSetModel.getTrackSet.foreach(_.removeTrackSubscriber(_self))
        if(newValue != null) {
          Context.trackSetModel.getTrackSet.find(_.channel.id == newValue.id).foreach(_.addTrackSubscriber(_self))
        }
        parentController.updateMonitorSession()
      }
    })
  }

  def start() = {
    println(s"Starting WebCam with source '${model.getSelectedSource}'...")

    if(currentWebCamTask != null && currentWebCamTask.isRunning) {
      currentWebCamTask.cancel()
      currentWebCamTask.stateProperty.addListener(new ChangeListener[State] {
        override def changed(observable: ObservableValue[_ <: State], oldValue: State, newValue: State) = {
          runThread()
        }
      })
    } else {
      runThread()
    }
  }

  def stop() = {
    println(s"Stopping active WebCam...")

    if(currentWebCamTask != null && currentWebCamTask.isRunning) {
      currentWebCamTask.cancel()
    }
  }

  private def runThread(): Unit = {
    println("Running thread")
    if(model.getSelectedSource != null) {
      currentWebCamTask = webCamTask(model.getSelectedSource.index)
      val thread = new Thread(currentWebCamTask)
      thread.setDaemon(true)
      thread.start()
    }
  }

  def webCamTask(index: Int) = new Task[Unit]() {
    override def call(): Unit = {
      try {
        val cam = Webcam.getWebcams.get(index)
        cam.open()

        val imageRef = new AtomicReference[WritableImage]()
        val decoratorRef = new AtomicReference[GraphicsDecorator]()
        while (!isCancelled) {
          val img = cam.getImage
          if (img != null) {
            imageRef.set(SwingFXUtils.toFXImage(img, imageRef.get()))
            decoratorRef.set(
              GraphicsDecorator({ case (gc: GraphicsContext, r: Rectangle) =>
                val gridSizeY = r.getHeight * 0.1
                val gridSizeX = r.getWidth * 0.1
                gc.clearRect(r.getX, r.getY, r.getWidth, r.getHeight)
                gc.setFont(new Font(gridSizeY))
                gc.setTextAlign(TextAlignment.CENTER)
                gc.setTextBaseline(VPos.CENTER)

                if (!model.isDisplayNoteDisabled) {
                  val textPositionY = r.getY + r.getHeight / 2 - gridSizeY * 2
                  val textCenterX = r.getX + r.getWidth / 2
                  val notes = activeNotes.toList.sortBy(x => (x._1.index, x._1.note.index))
                  val displayedNotes = notes.filter(n => n._2 == NoteActive || n._2 == NoteSustained)

                  def noteWidth(n: MusicNote): Double = {
                    if (model.isDisplayNoteInEnglish) {
                      Toolkit.getToolkit.getFontLoader.computeStringWidth(n.string, gc.getFont()) + gridSizeX * 0.3
                    } else if (model.isDisplayNoteInFixedDo) {
                      Toolkit.getToolkit.getFontLoader.computeStringWidth(n.fixedDoString, gc.getFont()) + gridSizeX * 0.3
                    } else {
                      0
                    }
                  }

                  val fullNotesWidth = (List(0.0) ++ displayedNotes.map(_._1.note).map(noteWidth)).sum

                  def textPositionX(i: Int) = {
                    textCenterX -
                      fullNotesWidth / 2 +
                      noteWidth(displayedNotes(i)._1.note).toInt / 2 +
                      (List(0.0) ++ displayedNotes.take(i).map(_._1.note).map(noteWidth)).sum
                  }

                  def text(kn: MusicNote) = {
                    if (model.isDisplayNoteInEnglish) {
                      kn.string
                    } else if (model.isDisplayNoteInFixedDo) {
                      kn.fixedDoString
                    } else {
                      ""
                    }
                  }


                  displayedNotes
                    .zipWithIndex
                    .foreach {
                      case ((note, NoteActive), i) =>
                        gc.setFill(Color.RED)
                        gc.setStroke(Color.WHITE)
                        gc.fillText(text(note.note), textPositionX(i), textPositionY.toInt)
                        gc.strokeText(text(note.note), textPositionX(i), textPositionY.toInt)
                      case ((note, NoteSustained), i) =>
                        gc.setFill(Color.RED.desaturate())
                        gc.setStroke(Color.WHITE)
                        gc.fillText(text(note.note), textPositionX(i), textPositionY.toInt)
                        gc.strokeText(text(note.note), textPositionX(i), textPositionY.toInt)
                      case _ =>
                    }
                }
              })
            )
            img.flush()

            Platform.runLater(new Runnable() {
              def run(): Unit = {
                model.setSourceImage(imageRef.get())
                model.setDecorator(decoratorRef.get())
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
        println(s"Finish task")
      } catch {
        case e: Exception =>
          println(s"Exception in WebCam Task thread: ${e.getMessage}")
          e.printStackTrace()
      }
    }
  }

  override def noteOn(kn: KeyboardNote): Unit = {
    activeNotes = activeNotes + (kn -> NoteActive)
  }

  override def noteOff(kn: KeyboardNote): Unit = {
    if(sustainActive) {
      activeNotes =
        activeNotes
          .map {
            case (note, NoteActive) => note -> NoteSustained
            case (note, status) => note -> status
          }
    } else {
      activeNotes = activeNotes.filterKeys(_ != kn)
    }
  }

  override def sustainOn(): Unit = {
    sustainActive = true
  }


  override def sustainOff(): Unit = {
    sustainActive = false
    activeNotes = activeNotes.filter(_._2 != NoteSustained)
  }
}
