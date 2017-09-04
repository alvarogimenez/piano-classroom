package ui.controller.component

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.beans.{InvalidationListener, Observable}
import javafx.event.EventHandler
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

import com.sun.javafx.geom.Rectangle

class Fader extends Pane {
  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  val minValue = 0.0
  val maxValue = dbToFaderPos(10)
  val position = new SimpleDoubleProperty()
  val atenuation = new SimpleDoubleProperty()
  var faderRenderArea: Rectangle = _
  var faderCollisionArea: Rectangle = _
  var faderCollisionAreaWidth = 100
  var faderDragActive = false
  var faderDragDeltaX = 0.0

  calculateRenderAreas()

  def getPosition: Double = position.get
  def setPosition(x: Double): Unit = position.set(x)
  def getPositionProperty: SimpleDoubleProperty = position

  def getAtenuation: Double = atenuation.get
  def setAtenuation(x: Double): Unit = atenuation.set(x)
  def getAtenuationProperty: SimpleDoubleProperty = atenuation

  position.addListener(new ChangeListener[Number]() {
    override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
      calculateRenderAreas()
      draw()
      atenuation.set(faderPosToDb(getPosition))
    }
  })
  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = {
      calculateRenderAreas()
      draw()
    }
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = {
      calculateRenderAreas()
      draw()
    }
  })
  canvas.setOnMousePressed(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      if(faderCollisionArea.contains(event.getX.toInt, event.getY.toInt)) {
        faderDragActive = true
        faderDragDeltaX = event.getX - getFaderCenterX()
      }
    }
  })
  canvas.setOnMouseReleased(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      if(faderDragActive) {
        faderDragActive = false
      } else {
        val minX = faderRenderArea.x + faderCollisionAreaWidth / 2
        val maxX = faderRenderArea.x + faderRenderArea.width - faderCollisionAreaWidth / 2
        val validPosition = Math.max(Math.min(maxX, event.getX), minX)
        setPosition((validPosition - minX) / (maxX - minX).toDouble * (maxValue - minValue))
      }
    }
  })
  canvas.setOnMouseDragged(new EventHandler[MouseEvent]() {
    override def handle(event: MouseEvent): Unit = {
      if(faderDragActive) {
        val minX = faderRenderArea.x + faderCollisionAreaWidth/2
        val maxX = faderRenderArea.x + faderRenderArea.width - faderCollisionAreaWidth/2
        val validPosition = Math.max(Math.min(maxX, event.getX - faderDragDeltaX), minX)
        setPosition((validPosition - minX)/(maxX - minX).toDouble * (maxValue - minValue))
      }
    }
  })

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())
    canvas.setWidth(snapSize(getWidth) - snappedLeftInset() - snappedRightInset())
    canvas.setHeight(snapSize(getHeight) - snappedTopInset() - snappedBottomInset())
    calculateRenderAreas()
  }

  private def getFaderCenterX() = {
    val minX = faderRenderArea.x + faderCollisionAreaWidth/2
    val maxX = faderRenderArea.x + faderRenderArea.width - faderCollisionAreaWidth/2
    minX + (maxX - minX)*getPosition/(maxValue - minValue)
  }

  private def calculateRenderAreas() = {
    faderRenderArea = new Rectangle(
      canvas.getLayoutX.toInt,
      canvas.getLayoutY.toInt,
      canvas.getWidth.toInt,
      canvas.getHeight.toInt)

    faderCollisionArea = new Rectangle(
      (getFaderCenterX() - faderCollisionAreaWidth/2).toInt,
      faderRenderArea.y,
      faderCollisionAreaWidth,
      faderRenderArea.height
    )
  }

  private def draw(): Unit = {
    val gc = canvas.getGraphicsContext2D
    gc.clearRect(0, 0, getWidth, getHeight)

    gc.setFill(Color.WHITE)
    gc.fillRect(faderRenderArea.x, faderRenderArea.y, faderRenderArea.width, faderRenderArea.height)

    gc.setStroke(Color.gray(0.7))
    gc.setLineWidth(1)
    gc.strokeRect(faderRenderArea.x, faderRenderArea.y, faderRenderArea.width, faderRenderArea.height)

    List(10, 5, 0, -5, -10, -20, -30, -40, -60, Double.NegativeInfinity)
      .foreach { db =>
        val relativePos = dbToFaderPos(db)
        val minX = faderRenderArea.x + faderCollisionAreaWidth/2
        val maxX = faderRenderArea.x + faderRenderArea.width - faderCollisionAreaWidth/2
        val posX = minX + (maxX - minX)*relativePos/(maxValue - minValue)
        gc.setStroke(Color.gray(0.5))
        gc.strokeLine(posX, faderCollisionArea.y, posX, faderCollisionArea.y + faderCollisionArea.height/2 - 10)
        gc.strokeLine(posX, faderCollisionArea.y + faderCollisionArea.height/2 + 10, posX, faderCollisionArea.y + faderCollisionArea.height)
        gc.setTextAlign(TextAlignment.CENTER)
        gc.setTextBaseline(VPos.CENTER)
        val text = if(db != Double.NegativeInfinity) {
          db.toInt.toString
        } else {
          "-Inf"
        }

        gc.strokeText(text, posX, getHeight/2)
      }


    def dDisplace(x: Double) = {
      gc.strokeLine(
        faderCollisionArea.x + x,
        faderCollisionArea.y,
        faderCollisionArea.x + x,
        faderCollisionArea.y + faderCollisionArea.height)
    }

    gc.setFill(new Color(0.9, 0.9, 0.9, 0.5))
    gc.fillRect(faderCollisionArea.x, faderCollisionArea.y, faderCollisionArea.width, faderCollisionArea.height)

    gc.setStroke(Color.CYAN)
    dDisplace(faderCollisionAreaWidth/2)

    gc.setLineWidth(2)
    gc.setStroke(Color.gray(0))
    gc.strokeRect(faderCollisionArea.x, faderCollisionArea.y, faderCollisionArea.width, faderCollisionArea.height)

    dDisplace(faderCollisionAreaWidth/2 - 10)
    dDisplace(faderCollisionAreaWidth/2 - 16)
    dDisplace(faderCollisionAreaWidth/2 - 20)
    dDisplace(faderCollisionAreaWidth/2 - 23)
    dDisplace(faderCollisionAreaWidth/2 + 10)
    dDisplace(faderCollisionAreaWidth/2 + 16)
    dDisplace(faderCollisionAreaWidth/2 + 20)
    dDisplace(faderCollisionAreaWidth/2 + 23)
    dDisplace(5)
    dDisplace(faderCollisionAreaWidth - 5)
  }

  def faderPosToDb(x: Double) = 20 * Math.log(x) / Math.log(1.8)
  def dbToFaderPos(x: Double) = Math.pow(1.8, x/20.0)

}