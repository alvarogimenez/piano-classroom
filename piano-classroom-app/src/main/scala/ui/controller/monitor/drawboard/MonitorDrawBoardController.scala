package ui.controller.monitor.drawboard

import javafx.beans.property.{SimpleListProperty, SimpleObjectProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ListChangeListener.Change
import javafx.collections.{FXCollections, ListChangeListener, ObservableList}
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Point2D
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.shape.Rectangle

import ui.controller.component.drawboard.{CanvasPreview, DrawBoardCanvas}
import ui.controller.monitor.GraphicsDecorator

import scala.collection.JavaConversions._

class MonitorDrawBoardModel {
  val decorator: SimpleObjectProperty[GraphicsDecorator] = new SimpleObjectProperty[GraphicsDecorator]()
  val draw_board_canvas_models_ol: ObservableList[DrawBoardCanvasModel] = FXCollections.observableArrayList[DrawBoardCanvasModel]
  val draw_board_canvas_models: SimpleListProperty[DrawBoardCanvasModel] = new SimpleListProperty[DrawBoardCanvasModel](draw_board_canvas_models_ol)
  val selected_draw_board_canvas_model: SimpleObjectProperty[DrawBoardCanvasModel] = new SimpleObjectProperty[DrawBoardCanvasModel]()

  def getDecorator: GraphicsDecorator = decorator.get
  def setDecorator(d: GraphicsDecorator): Unit = decorator.set(d)
  def getDecoratorProperty: SimpleObjectProperty[GraphicsDecorator] = decorator

  def getDrawBoardCanvasModels: List[DrawBoardCanvasModel] = draw_board_canvas_models.get().toList
  def setDrawBoardCanvasModels(m: List[DrawBoardCanvasModel]) = draw_board_canvas_models_ol.setAll(m)
  def getDrawBoardCanvasModelProperty: SimpleListProperty[DrawBoardCanvasModel] = draw_board_canvas_models

  def getSelectedDrawBoardCanvasModel: DrawBoardCanvasModel = selected_draw_board_canvas_model.get()
  def setSelectedDrawBoardCanvasModel(d: DrawBoardCanvasModel): Unit = selected_draw_board_canvas_model.set(d)
  def getSelectedDrawBoardCanvasModelProperty: SimpleObjectProperty[DrawBoardCanvasModel] = selected_draw_board_canvas_model
}

class MonitorDrawBoardController(model: MonitorDrawBoardModel) {
  @FXML var hbox_available_canvas: HBox = _
  @FXML var bpane_main: BorderPane = _

  def initialize() = {
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
      }
    })

    model.getSelectedDrawBoardCanvasModelProperty.addListener(new ChangeListener[DrawBoardCanvasModel] {
      override def changed(observable: ObservableValue[_ <: DrawBoardCanvasModel], oldValue: DrawBoardCanvasModel, newValue: DrawBoardCanvasModel): Unit = {
        model.getDecoratorProperty.unbind()
        if(newValue != null) {
          model.getDecoratorProperty.bind(newValue.decorator)
          val drawBoardCanvas = new DrawBoardCanvas(newValue)
          bpane_main.setCenter(drawBoardCanvas)
        }
      }
    })

    model.setDrawBoardCanvasModels((0 to 10)
      .map { i =>
        val m = new DrawBoardCanvasModel()
        m.setCanvasData(CanvasData(
          name = s"Canvas $i",
          aspectRatio = 4.0/3.0,
          fullscreenViewport = new Rectangle(0, 0, 100, 100),
          shapes = Set.empty
        ))
        m
      }.toList)
  }

  def start() = {

  }

  def stop() = {

  }
}
