package ui.controller.track

import javafx.beans.{InvalidationListener, Observable}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

import com.sun.javafx.geom.Rectangle
import util.{KeyboardNote, MusicNote}
import util.MusicNote.MusicNote

class KeyboardCanvas extends Pane {
  var activeNotes: Set[KeyboardNote] = Set.empty

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })

  def queueActiveNote(n: KeyboardNote): Unit = {
    activeNotes += n
    draw()
  }

  def dequeueActiveNote(n: KeyboardNote): Unit = {
    activeNotes = activeNotes - n
    draw()
  }

  override def layoutChildren(): Unit = {
    super.layoutChildren()
    canvas.setLayoutX(snappedLeftInset())
    canvas.setLayoutY(snappedTopInset())
    canvas.setWidth(snapSize(getWidth) - snappedLeftInset() - snappedRightInset())
    canvas.setHeight(snapSize(getHeight) - snappedTopInset() - snappedBottomInset())
  }

  private def draw(): Unit = {
    val gc = canvas.getGraphicsContext2D
    gc.clearRect(0, 0, getWidth, getHeight)
    drawKeyboard(gc, KeyboardNote(MusicNote.A, 0), KeyboardNote(MusicNote.C, 8), new Rectangle(0, 0, getWidth.toInt, getHeight.toInt))
  }

  private def drawKeyboard(gc: GraphicsContext, fromNote: KeyboardNote, toNote: KeyboardNote, r: Rectangle) = {
    val from =
      if(isLower(fromNote.note)) {
        fromNote.absoluteIndex()
      } else {
        fromNote.absoluteIndex() + 1
      }

    val to =
      if(isLower(toNote.note)) {
        toNote.absoluteIndex()
      } else {
        toNote.absoluteIndex() + 1
      }

    val notes = (from to to).map(KeyboardNote.widthAbsoluteIndex)
    val lowerNotes = notes.filter(n => isLower(n.note))
    val upperNotes = notes.filter(n => !isLower(n.note))
    val lowerNoteWidth = r.width / lowerNotes.size.toDouble
    val upperNoteWidth = lowerNoteWidth * 0.6
    val lowerNoteHeight = r.height
    val upperNoteHeight = lowerNoteHeight * 0.6
    val offsetLeft = displacementFromKeyborardNote(lowerNotes.head)

    gc.setStroke(Color.BLACK)
    gc.setFill(Color.WHITE)
    gc.fillRect(r.x, r.y, r.width, r.height)
    lowerNotes
      .foreach { n =>
        val offset = displacementFromKeyborardNote(n) - offsetLeft
        if(activeNotes.contains(n)) {
          gc.setFill(Color.LIGHTBLUE)
          gc.fillRect(r.x + offset*lowerNoteWidth, r.y, lowerNoteWidth, lowerNoteHeight)
          gc.strokeRect(r.x + offset*lowerNoteWidth, r.y, lowerNoteWidth, lowerNoteHeight)
        } else {
          gc.setFill(Color.WHITE)
          gc.strokeRect(r.x + offset*lowerNoteWidth, r.y, lowerNoteWidth, lowerNoteHeight)
        }
      }

    upperNotes
      .foreach { n =>
        val offset = displacementFromKeyborardNote(n) - offsetLeft
        if(activeNotes.contains(n)) {
          gc.setFill(Color.LIGHTBLUE)
          gc.fillRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
          gc.strokeRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
        } else {
          gc.setFill(new Color(0.2, 0.2, 0.2, 1.0))
          gc.fillRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
        }
      }
  }

  private def displacementFromKeyborardNote(kn: KeyboardNote): Float = {
    7f*kn.index + musicNoteToOffset(kn.note)
  }

  private def isLower(mn: MusicNote): Boolean = {
    mn match {
      case MusicNote.C => true
      case MusicNote.`C#-Db` => false
      case MusicNote.D => true
      case MusicNote.`D#-Eb` => false
      case MusicNote.E => true
      case MusicNote.F => true
      case MusicNote.`F#-Gb` => false
      case MusicNote.G => true
      case MusicNote.`G#-Ab` => false
      case MusicNote.A => true
      case MusicNote.`A#-Bb` => false
      case MusicNote.B => true
    }
  }

  private def musicNoteToOffset(mn: MusicNote): Float = {
    mn match {
      case MusicNote.C => 0f
      case MusicNote.`C#-Db` => 0.5f
      case MusicNote.D => 1f
      case MusicNote.`D#-Eb` => 1.5f
      case MusicNote.E => 2f
      case MusicNote.F => 3f
      case MusicNote.`F#-Gb` => 3.5f
      case MusicNote.G => 4f
      case MusicNote.`G#-Ab` => 4.5f
      case MusicNote.A => 5f
      case MusicNote.`A#-Bb` => 5.5f
      case MusicNote.B => 6f
    }
  }
}