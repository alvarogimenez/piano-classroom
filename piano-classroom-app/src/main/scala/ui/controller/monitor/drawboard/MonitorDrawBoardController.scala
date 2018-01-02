package ui.controller.monitor.drawboard

import javafx.beans.property.{SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, ChoiceBox, ColorPicker, ScrollPane}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ui.controller.ProjectSessionUpdating
import ui.controller.component.PaletteColorButton
import ui.controller.component.drawboard.{CanvasPreview, DrawBoardCanvas, Pen}
import ui.controller.monitor.GraphicsDecorator
import ui.controller.monitor.drawboard.MonitorDrawBoardModel.PenSizeMillis

import scala.collection.JavaConversions._

object MonitorDrawBoardModel {
  case class PenSizeMillis(size: Int) {
    override def toString: String = size.toString
  }
}
class MonitorDrawBoardModel {
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()
  val draw_board_canvas_models_ol: ObservableList[DrawBoardCanvasModel] = FXCollections.observableArrayList[DrawBoardCanvasModel]
  val draw_board_canvas_models: SimpleListProperty[DrawBoardCanvasModel] = new SimpleListProperty[DrawBoardCanvasModel](draw_board_canvas_models_ol)
  val available_color_buttons_ol: ObservableList[PaletteColorButton] = FXCollections.observableArrayList[PaletteColorButton]
  val available_color_buttons: SimpleListProperty[PaletteColorButton] = new SimpleListProperty[PaletteColorButton](available_color_buttons_ol)
  val selected_draw_board_canvas_model: SimpleObjectProperty[DrawBoardCanvasModel] = new SimpleObjectProperty[DrawBoardCanvasModel]()
  val pen_size_values_ol: ObservableList[PenSizeMillis] = FXCollections.observableArrayList[PenSizeMillis]
  val pen_size_values: SimpleListProperty[PenSizeMillis] = new SimpleListProperty[PenSizeMillis](pen_size_values_ol)
  val selected_pen_size: SimpleObjectProperty[PenSizeMillis] = new SimpleObjectProperty[PenSizeMillis]()
  val selected_pen: SimpleObjectProperty[Pen] = new SimpleObjectProperty[Pen]()

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator

  def getSelectedPen: Pen = selected_pen.get
  def setSelectedPen(d: Pen): Unit = selected_pen.set(d)
  def getSelectedPenProperty: SimpleObjectProperty[Pen] = selected_pen

  def getDrawBoardCanvasModels: List[DrawBoardCanvasModel] = draw_board_canvas_models.get().toList
  def setDrawBoardCanvasModels(m: List[DrawBoardCanvasModel]) = draw_board_canvas_models_ol.setAll(m)
  def addDrawBoardCanvasModel(m: DrawBoardCanvasModel) = draw_board_canvas_models_ol.add(m)
  def removeDrawBoardCanvasModel(m: DrawBoardCanvasModel) = draw_board_canvas_models_ol.remove(m)
  def getDrawBoardCanvasModelProperty: SimpleListProperty[DrawBoardCanvasModel] = draw_board_canvas_models

  def getSelectedDrawBoardCanvasModel: DrawBoardCanvasModel = selected_draw_board_canvas_model.get()
  def setSelectedDrawBoardCanvasModel(d: DrawBoardCanvasModel): Unit = selected_draw_board_canvas_model.set(d)
  def getSelectedDrawBoardCanvasModelProperty: SimpleObjectProperty[DrawBoardCanvasModel] = selected_draw_board_canvas_model

  def getPenSizeValues: List[PenSizeMillis] = pen_size_values.get().toList
  def setPenSizeValues(v: List[PenSizeMillis]):Unit = pen_size_values.setAll(v)
  def getPenSizeValuesProperty: SimpleListProperty[PenSizeMillis] = pen_size_values

  def getSelectedPenSize: PenSizeMillis = selected_pen_size.get()
  def setSelectedPenSize(s: PenSizeMillis) = selected_pen_size.set(s)
  def getSelectedPenSizeProperty: SimpleObjectProperty[PenSizeMillis] = selected_pen_size

  def getAvailableColorButtons: List[PaletteColorButton] = available_color_buttons.get().toList
  def setAvailableColorButtons(v: List[PaletteColorButton]):Unit = available_color_buttons.setAll(v)
  def addAvailableColorButtons(v: PaletteColorButton):Unit = available_color_buttons.add(v)
  def removeAvailableColorButtons(v: PaletteColorButton):Unit = available_color_buttons.remove(v)
  def getAvailableColorButtonsProperty: SimpleListProperty[PaletteColorButton] = available_color_buttons
}

class MonitorDrawBoardController(parentController: ProjectSessionUpdating, model: MonitorDrawBoardModel) {
  @FXML var hbox_available_canvas: HBox = _
  @FXML var scrollpane_palette: ScrollPane = _
  @FXML var bpane_main: BorderPane = _
  @FXML var vbox_palette: VBox = _
  @FXML var color_picker: ColorPicker = _
  @FXML var choicebox_pen_millis: ChoiceBox[PenSizeMillis] = _
  @FXML var button_add_color: Button = _
  @FXML var button_add_canvas: Button = _
  @FXML var button_remove_canvas: Button = _

  @FXML var button_action_clear: Button = _
  @FXML var button_action_undo: Button = _
  @FXML var button_action_redo: Button = _

  def initialize() = {
    model.setSelectedPen(Pen(3.0 / 1000.0, Color.BLACK))

    model.getSelectedDrawBoardCanvasModelProperty.addListener(new ChangeListener[DrawBoardCanvasModel] {
      override def changed(observable: ObservableValue[_ <: DrawBoardCanvasModel], oldValue: DrawBoardCanvasModel, newValue: DrawBoardCanvasModel) = {
        if(newValue != null) {
          newValue.setSelected(true)
        }
        if(oldValue != null) {
          oldValue.setSelected(false)
        }
      }
    })

    model.getDrawBoardCanvasModelProperty.addListener(new ListChangeListener[DrawBoardCanvasModel] {
      override def onChanged(c: Change[_ <: DrawBoardCanvasModel]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { trackModel =>
                val track = new CanvasPreview(trackModel)
                track.setOnMouseClicked(new EventHandler[MouseEvent] {
                  override def handle(event: MouseEvent) = {
                    model.setSelectedDrawBoardCanvasModel(trackModel)
                  }
                })
                track.setUserData(trackModel)
                hbox_available_canvas.getChildren.add(track)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { trackModel =>
                hbox_available_canvas.getChildren.find(_.getUserData == trackModel) match {
                  case Some(track) =>
                    hbox_available_canvas.getChildren.remove(track)
                  case _ =>
                }
              }
          }
        }

        parentController.updateProjectSession()
      }
    })

    model.getAvailableColorButtonsProperty.addListener(new ListChangeListener[PaletteColorButton] {
      override def onChanged(c: Change[_ <: PaletteColorButton]) = {
        while (c.next()) {
          if (c.getAddedSize != 0) {
            c.getAddedSubList
              .foreach { colorButton =>
                colorButton.addEventHandler(MouseEvent.ANY, new EventHandler[MouseEvent]() {
                  override def handle(event: MouseEvent): Unit = {
                    scrollpane_palette.fireEvent(event)
                    vbox_palette.fireEvent(event)
                  }
                })
                colorButton.setOnMouseClicked(new EventHandler[MouseEvent] {
                  override def handle(event: MouseEvent) = {
                    model.setSelectedPen(
                      Pen(
                        size = colorButton.millis / 1000.0,
                        color = colorButton.color
                      )
                    )
                  }
                })
                colorButton.setOnDelete(new EventHandler[ActionEvent] {
                  override def handle(event: ActionEvent) = {
                    model.removeAvailableColorButtons(colorButton)
                  }
                })
                vbox_palette.getChildren.add(colorButton)
              }
          } else if (c.getRemovedSize != 0) {
            c.getRemoved
              .map { colorButton =>
                vbox_palette.getChildren.remove(colorButton)
              }
          }
        }

        parentController.updateProjectSession()
      }
    })

    model.getSelectedDrawBoardCanvasModelProperty.addListener(new ChangeListener[DrawBoardCanvasModel] {
      override def changed(observable: ObservableValue[_ <: DrawBoardCanvasModel], oldValue: DrawBoardCanvasModel, newValue: DrawBoardCanvasModel): Unit = {
        model.getDecoratorProperty.unbind()
        if(newValue != null) {
          newValue.getPenProperty.unbind()
          newValue.getPenProperty.bind(model.getSelectedPenProperty)
          model.getDecoratorProperty.bind(newValue.decorator)
          val drawBoardCanvas = new DrawBoardCanvas(newValue)
          drawBoardCanvas.setUpdateHandler(new EventHandler[MouseEvent] {
            override def handle(event: MouseEvent) = parentController.updateProjectSession()
          })
          bpane_main.setCenter(drawBoardCanvas)
        }

        parentController.updateProjectSession()
      }
    })

    val availablePenSizes = (1 to 15).toList.map(PenSizeMillis)
    model.setPenSizeValues(availablePenSizes)
    model.setSelectedPenSize(availablePenSizes.find(_.size == 3).get)
    choicebox_pen_millis.itemsProperty().bind(model.getPenSizeValuesProperty)
    choicebox_pen_millis.valueProperty().bindBidirectional(model.getSelectedPenSizeProperty)

    button_add_color.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        val selectedColor = color_picker.getValue
        val selectedSize = model.getSelectedPenSize.size
        val colorButton = new PaletteColorButton(selectedColor, selectedSize)

        model.addAvailableColorButtons(colorButton)
        parentController.updateProjectSession()
      }
    })

    button_add_canvas.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        val m = new DrawBoardCanvasModel()
        m.setCanvasData(CanvasData(
          name = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss").print(new DateTime()),
          aspectRatio = 4.0/3.0,
          fullscreenViewport = new Rectangle(0, 0, 100, 100),
          shapes = Set.empty
        ))
        model.addDrawBoardCanvasModel(m)
      }
    })

    button_remove_canvas.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        if(model.getSelectedDrawBoardCanvasModel != null) {
          model.removeDrawBoardCanvasModel(model.getSelectedDrawBoardCanvasModel)
        }
      }
    })

    button_action_clear.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent) = {
        if(model.getSelectedDrawBoardCanvasModel != null) {
          model.getSelectedDrawBoardCanvasModel.setCanvasData(
            model.getSelectedDrawBoardCanvasModel.getCanvasData.copy(shapes = Set.empty)
          )
          parentController.updateProjectSession()
        }
      }
    })
  }

  def start() = {

  }

  def stop() = {

  }
}
