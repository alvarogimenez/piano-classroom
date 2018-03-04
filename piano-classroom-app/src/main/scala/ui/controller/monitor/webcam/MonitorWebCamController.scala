package ui.controller.monitor.webcam

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.lang.Boolean
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.beans.{InvalidationListener, Observable}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.concurrent.Task
import javafx.concurrent.Worker.State
import javafx.embed.swing.SwingFXUtils
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.geometry.VPos
import javafx.scene.Scene
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView, WritableImage}
import javafx.scene.layout.{BorderPane, StackPane}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, TextAlignment}
import javafx.stage.{Modality, Stage}

import com.github.sarxos.webcam.Webcam
import com.sun.javafx.tk.Toolkit
import context.Context
import sound.audio.channel.{Channel, MidiChannel}
import ui.controller.global.ProjectSessionUpdating
import ui.controller.monitor._
import ui.controller.monitor.highlighterConfiguration.{HighlighterConfigurationController, HighlighterConfigurationModel, _}
import ui.controller.track.pianoRange.MidiEventSubscriber
import util.KeyboardLayoutUtils.{KeyboardLayout, LayoutMode}
import util.MusicNote.MusicNote
import util._

import scala.collection.JavaConversions._

class MonitorWebCamModel {
  val sources_ol: ObservableList[WebCamSource] = FXCollections.observableArrayList[WebCamSource]
  val sources: SimpleListProperty[WebCamSource] = new SimpleListProperty[WebCamSource](sources_ol)
  val selected_source: SimpleObjectProperty[WebCamSource] = new SimpleObjectProperty[WebCamSource]()
  val track_note_sources_ol: ObservableList[ChannelSource] = FXCollections.observableArrayList[ChannelSource]
  val track_note_sources: SimpleListProperty[ChannelSource] = new SimpleListProperty[ChannelSource](track_note_sources_ol)
  val track_note_selected_source: SimpleObjectProperty[ChannelSource] = new SimpleObjectProperty[ChannelSource]()
  val source_image: SimpleObjectProperty[Image] = new SimpleObjectProperty[Image]()
  val source_raw_image: SimpleObjectProperty[BufferedImage] = new SimpleObjectProperty[BufferedImage]()
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()
  val displayNoteDisabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val displayNoteInEnglish: SimpleBooleanProperty = new SimpleBooleanProperty()
  val displayNoteInFixedDo: SimpleBooleanProperty = new SimpleBooleanProperty()
  val sustain_visble: SimpleBooleanProperty = new SimpleBooleanProperty()
  val highlight_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val highlight_subtractive: SimpleBooleanProperty = new SimpleBooleanProperty()
  val highlight_subtractive_sensibility: DoubleProperty = new SimpleDoubleProperty()
  var keyboard_layout: ObjectProperty[KeyboardLayout] = new SimpleObjectProperty[KeyboardLayout]()

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

  def getSourceRawImage: BufferedImage = source_raw_image.get()
  def setSourceRawImage(i: BufferedImage): Unit = source_raw_image.set(i)
  def getSourceRawImageProperty: SimpleObjectProperty[BufferedImage] = source_raw_image

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

  def isSustainActive: Boolean = sustain_visble.get
  def setSustainActive(d: Boolean): Unit = sustain_visble.set(d)
  def getSustainActiveProperty: SimpleBooleanProperty = sustain_visble

  def isHighlightEnabled: Boolean = highlight_enabled.get
  def setHighlightEnabled(d: Boolean): Unit = highlight_enabled.set(d)
  def getHighlightEnabledProperty: SimpleBooleanProperty = highlight_enabled

  def isHighlightSubtractive: Boolean = highlight_subtractive.get
  def setHighlightSubtractive(d: Boolean): Unit = highlight_subtractive.set(d)
  def getHighlightSubtractiveProperty: SimpleBooleanProperty = highlight_subtractive

  def getHighlightSubtractiveSensibility: Double = highlight_subtractive_sensibility.get
  def setHighlightSubtractiveSensibility(h: Double): Unit = highlight_subtractive_sensibility.set(h)
  def getHighlightSubtractiveSensibilityProperty: DoubleProperty = highlight_subtractive_sensibility

  def getKeyboardLayout: KeyboardLayout = keyboard_layout.get
  def setKeyboardLayout(b: KeyboardLayout): Unit = keyboard_layout.set(b)
  def getKeyboardLayoutProperty: ObjectProperty[KeyboardLayout] = keyboard_layout

  setDisplayNoteDisabled(true)
  setTrackNoteSources(List(null))

  Context.channelService.getChannelsProperty.addListener(new ListChangeListener[Channel] {
    override def onChanged(c: Change[_ <: Channel]) = {
      while (c.next()) {
        if (c.getAddedSize != 0) {
          c.getAddedSubList
            .foreach { channel =>
              recreateSourcesFromModel()
              channel.getNameProperty.addListener(new InvalidationListener {
                override def invalidated(observable: Observable) = {
                  recreateSourcesFromModel()
                }
              })
            }
        } else if (c.getRemovedSize != 0) {
          c.getRemoved
            .map { channel =>
              recreateSourcesFromModel()
            }
        }
      }
    }
  })

  def recreateSourcesFromModel() = {
    setTrackNoteSources(List(null) ++ Context.channelService.getChannels.map(c => ChannelSource(c.getName, c.getId)))
  }
}

class MonitorWebCamController(parentController: ProjectSessionUpdating, model: MonitorWebCamModel) extends MidiEventSubscriber {
  @FXML var stackpane: StackPane = _
  @FXML var imageview_webcam: ImageView = _
  @FXML var canvas_overlay: Canvas = _
  @FXML var combobox_source: ComboBox[WebCamSource] = _
  @FXML var combobox_note_helper_source: ComboBox[ChannelSource] = _

  @FXML var toggle_note_display_no_display: ToggleButton = _ 
  @FXML var toggle_note_display_english: ToggleButton = _ 
  @FXML var toggle_note_display_fixed_do: ToggleButton = _ 
  @FXML var checkbox_sustain_visible: CheckBox = _

  @FXML var checkbox_key_highlighter_enabled: CheckBox = _
  @FXML var checkbox_key_highlighter_subtractive: CheckBox = _
  @FXML var button_key_highlighter: Button = _
  @FXML var slider_key_highlighter_sensibility: Slider = _

  var currentWebCamTask: Task[Unit] = _
  var _self = this


  @volatile var activeNotes: Map[KeyboardNote, NoteStatus] = Map.empty
  @volatile var sustainActive = false

  def initialize() = {
    imageview_webcam.setPreserveRatio(true)
    imageview_webcam.fitWidthProperty().bind(stackpane.widthProperty())
    canvas_overlay.widthProperty().bind(stackpane.widthProperty())
    canvas_overlay.heightProperty().bind(stackpane.heightProperty())

    toggle_note_display_no_display.selectedProperty.bindBidirectional(model.getDisplayNoteDisabledProperty)
    toggle_note_display_english.selectedProperty().bindBidirectional(model.getDisplayNoteInEnglishProperty)
    toggle_note_display_fixed_do.selectedProperty().bindBidirectional(model.getDisplayNoteInFixedDoProperty)

    checkbox_sustain_visible.selectedProperty().bindBidirectional(model.getSustainActiveProperty)
    checkbox_key_highlighter_enabled.selectedProperty().bindBidirectional(model.getHighlightEnabledProperty)
    checkbox_key_highlighter_subtractive.selectedProperty().bindBidirectional(model.getHighlightSubtractiveProperty)
    slider_key_highlighter_sensibility.valueProperty().bindBidirectional(model.getHighlightSubtractiveSensibilityProperty)
    slider_key_highlighter_sensibility.disableProperty().bind(
      Bindings.createBooleanBinding(
        new Callable[Boolean] {
          override def call() = {
            !model.isHighlightEnabled || !model.isHighlightSubtractive
          }
        },
        model.getHighlightEnabledProperty,
        model.getHighlightSubtractiveProperty
      )
    )
    checkbox_key_highlighter_subtractive.disableProperty().bind(
      Bindings.createBooleanBinding(
        new Callable[Boolean] {
          override def call() = {
            !model.isHighlightEnabled
          }
        },
        model.getHighlightEnabledProperty
      )
    )

    button_key_highlighter.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        val dialog = new Stage()
        val loader = new FXMLLoader()
        val _model = new HighlighterConfigurationModel()
        _model.getImageProperty.bind(model.getSourceRawImageProperty)
        _model.setPreview(model.getSourceRawImage)
        if(model.getKeyboardLayout != null) {
          _model.setKeyboardLayout(model.getKeyboardLayout)
          _model.setBrightnessThreshold(model.getKeyboardLayout.brightnessThreshold * 100)
          _model.setSmoothAverage(model.getKeyboardLayout.smoothAverage * 100)
          _model.setCutY(model.getKeyboardLayout.cutY)
        }

        val _controller = new HighlighterConfigurationController(dialog, _model)
        loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/dialogs/MonitorKeyboardLayout.fxml"))
        loader.setController(_controller)

        dialog.setScene(new Scene(loader.load().asInstanceOf[BorderPane]))
        dialog.setResizable(false)
        dialog.setTitle("Configure Key Highlighter")
        dialog.initOwner(Context.primaryStage)
        dialog.initModality(Modality.APPLICATION_MODAL)
        dialog.showAndWait()

        if(_model.getExitStatus == HIGHLIGHTER_CONFIGURATION_MODAL_ACCEPT) {
          model.setKeyboardLayout(_model.getKeyboardLayout)
          parentController.updateProjectSession()
        }
      }
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

        parentController.updateProjectSession()
      }
    })

    model.getTrackNoteSelectedSourceProperty.addListener(new ChangeListener[ChannelSource] {
      override def changed(observable: ObservableValue[_ <: ChannelSource], oldValue: ChannelSource, newValue: ChannelSource) = {
        Context
          .channelService
          .getChannels
          .find(_.getId == newValue.id)
          .collect {
            case channel: MidiChannel => channel
          }
          .foreach(_.removeMidiSubscriber(_self))

        activeNotes = Map.empty
        if(newValue != null) {
          Context
            .channelService
            .getChannels
            .find(_.getId == newValue.id)
            .collect {
              case channel: MidiChannel => channel
            }
            .foreach(_.addMidiSubscriber(_self))
        }
        parentController.updateProjectSession()
      }
    })

    model.getDisplayNoteDisabledProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getDisplayNoteInEnglishProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getDisplayNoteInFixedDoProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getSustainActiveProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getHighlightEnabledProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getHighlightSubtractiveProperty.addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = parentController.updateProjectSession()
    })
    model.getHighlightSubtractiveSensibilityProperty.addListener(new ChangeListener[Any] {
      override def changed(observable: ObservableValue[_ <: Any], oldValue: Any, newValue: Any) = parentController.updateProjectSession()
    })
    model.getKeyboardLayoutProperty.addListener(new ChangeListener[Any] {
      override def changed(observable: ObservableValue[_ <: Any], oldValue: Any, newValue: Any) = parentController.updateProjectSession()
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
        if(!cam.isOpen) {
          cam.setViewSize(new Dimension(640, 480))
          cam.open()
        }

        val imageRef = new AtomicReference[WritableImage]()
        val rawImageRef = new AtomicReference[BufferedImage]()
        val decoratorRef = new AtomicReference[GraphicsDecorator]()
        val sustainOff = new Image(getClass.getResourceAsStream("/assets/icon/SustainUp.png"))
        val sustainOn = new Image(getClass.getResourceAsStream("/assets/icon/SustainDown.png"))

        while (!isCancelled) {
          val img: BufferedImage = cam.getImage

          if (img != null) {
            if(model.isHighlightEnabled) {
              imageRef.set(
                SwingFXUtils.toFXImage(
                  KeyboardLayoutUtils.paintLayoutActiveNotes(
                    img = img,
                    keyboardLayout = model.getKeyboardLayout,
                    activeNotes = activeNotes,
                    mode = if(model.isHighlightSubtractive) {
                      LayoutMode.Subtractive
                    } else {
                      LayoutMode.FullLayout
                    },
                    sensibility = model.getHighlightSubtractiveSensibility / 100
                  ),
                  imageRef.get()
                )
              )
            } else {
              imageRef.set(
                SwingFXUtils.toFXImage(img, imageRef.get())
              )
            }

            rawImageRef.set(img)

            decoratorRef.set(
              GraphicsDecorator({ case (gc: GraphicsContext, r: Rectangle) =>
                val gridSizeY = r.getHeight * 0.1
                val gridSizeX = r.getWidth * 0.1
                gc.clearRect(r.getX, r.getY, r.getWidth, r.getHeight)
                gc.setFont(new Font(gridSizeY))
                gc.setTextAlign(TextAlignment.CENTER)
                gc.setTextBaseline(VPos.CENTER)

                // Show currently pressed notes
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

                // Show current sustain pedal status
                if(model.isSustainActive) {
                  val img =
                    if(sustainActive ){
                      sustainOn
                    } else {
                      sustainOff
                    }

                  gc.drawImage(
                    img,
                    r.getX + gridSizeX * 5.5,
                    r.getY + gridSizeY * 1,
                    gridSizeX * 4,
                    (gridSizeX * 4) *sustainOff.getHeight / sustainOff.getWidth)
                }
              })
            )
            img.flush()

            Platform.runLater(new Runnable() {
              def run(): Unit = {
                model.setSourceRawImage(rawImageRef.get())
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
            case (note, NoteActive) if note == kn => note -> NoteSustained
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
