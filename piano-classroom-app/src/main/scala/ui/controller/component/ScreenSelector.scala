package ui.controller.component

import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Screen

import com.sun.javafx.geom.Rectangle

import scala.collection.JavaConversions._
import scala.util.Random

class ScreenSelector extends Pane {
  val colorSeed = System.currentTimeMillis()
  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  val selected_screen : SimpleObjectProperty[Option[ScreenDefinition]] = new SimpleObjectProperty[Option[ScreenDefinition]](None)
  def getSelectedScreen: Option[ScreenDefinition] = selected_screen.get
  def setSelectedScreen(x: Option[ScreenDefinition]) = selected_screen.set(x)
  def getSelectedScreenProperty: SimpleObjectProperty[Option[ScreenDefinition]] = selected_screen

  this.setOnMouseClicked(new EventHandler[MouseEvent] {
    override def handle(event: MouseEvent): Unit = {
      setSelectedScreen(calculateScreens().find(_.bounds.contains(event.getX.toInt, event.getY.toInt)))
      draw()
    }
  })

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())
    canvas.setWidth(snapSize(getWidth) - snappedLeftInset() - snappedRightInset())
    canvas.setHeight(snapSize(getHeight) - snappedTopInset() - snappedBottomInset())
    draw()
  }

  private def draw(): Unit = {
    val gc = canvas.getGraphicsContext2D
    gc.clearRect(0, 0, getWidth, getHeight)

    gc.setStroke(Color.GRAY)
    gc.setLineWidth(1)
    gc.strokeRect(0, 0, getWidth, getHeight)
    gc.setFill(Color.LIGHTGRAY)
    gc.fillRect(0, 0, getWidth, getHeight)

    calculateScreens()
      .sortWith((a, b) => a.index != getSelectedScreen.map(_.index).getOrElse(-1))
      .foreach { b =>

        gc.setFill(b.color.desaturate().brighter())
        gc.fillRect(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height)

        getSelectedScreen match {
          case Some(s) if s.index == b.index =>
            gc.setStroke(Color.CYAN)
            gc.setLineDashes(10, 10)
            gc.setLineWidth(5)
            gc.strokeRect(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height)
          case _ =>
            gc.setStroke(b.color)
            gc.setLineDashes()
            gc.setLineWidth(2)
            gc.strokeRect(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height)
        }
      }
  }

  case class ScreenDefinition(
    index: Int,
    color: Color,
    bounds: Rectangle,
    screen: Screen
  )

  private def calculateScreens(): List[ScreenDefinition] = {
    val screens: Map[Int, Screen] =
        Screen.getScreens
        .toList
        .zipWithIndex
        .map { case (screen, index) => (index, screen)}
        .toMap
    val minX = screens.values.map(_.getBounds.getMinX).min.toInt
    val minY = screens.values.map(_.getBounds.getMinY).min.toInt
    val maxX = screens.values.map(_.getBounds.getMaxX).max.toInt
    val maxY = screens.values.map(_.getBounds.getMaxY).max.toInt
    val boundingRect = new Rectangle(minX, minY, maxX - minX, maxY - minY)
    val sWidth = getWidth / boundingRect.width
    val sHeight = getHeight / boundingRect.height
    val sFactor = Math.min(sWidth, sHeight)
    screens
      .toList
      .map { case (index, screen) =>
        val centerX = getWidth / 2
        val centerY = getHeight / 2
        val r = new Random(index * colorSeed)
        ScreenDefinition(
          index = index,
          color = new Color(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1),
          bounds = new Rectangle(
            (centerX - boundingRect.width*sFactor/2 + (screen.getBounds.getMinX - minX)*sFactor).toInt,
            (centerY - boundingRect.height*sFactor/2  + (screen.getBounds.getMinY - minY)*sFactor).toInt,
            ((screen.getBounds.getMaxX - screen.getBounds.getMinX)*sFactor).toInt,
            ((screen.getBounds.getMaxY - screen.getBounds.getMinY)*sFactor).toInt
          ),
          screen = screen
        )
      }
  }
}