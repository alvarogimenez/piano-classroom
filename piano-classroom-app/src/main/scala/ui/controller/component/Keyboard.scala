package ui.controller.component

import javafx.application.Platform
import javafx.beans.property.{SimpleBooleanProperty, SimpleObjectProperty}
import javafx.beans.{InvalidationListener, Observable}
import javafx.concurrent.Task
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

import com.sun.javafx.geom.Rectangle
import util.MusicNote.MusicNote
import util.{KeyboardNote, MusicNote}

class Keyboard extends Pane {
  val piano_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val piano_roll_enabled: SimpleBooleanProperty = new SimpleBooleanProperty()
  val start_note: SimpleObjectProperty[KeyboardNote] = new SimpleObjectProperty[KeyboardNote]()
  val end_note: SimpleObjectProperty[KeyboardNote] = new SimpleObjectProperty[KeyboardNote]()

  def getPianoEnabled: scala.Boolean = piano_enabled.get
  def setPianoEnabled(e: scala.Boolean): Unit = piano_enabled.set(e)
  def getPianoEnabledProperty: SimpleBooleanProperty = piano_enabled

  def getPianoRollEnabled: scala.Boolean = piano_roll_enabled.get
  def setPianoRollEnabled(e: scala.Boolean): Unit = piano_roll_enabled.set(e)
  def getPianoRollEnabledProperty: SimpleBooleanProperty = piano_roll_enabled

  def getStartNote: KeyboardNote = start_note.get
  def setStartNote(n: KeyboardNote): Unit = start_note.set(n)
  def getStartNoteProperty: SimpleObjectProperty[KeyboardNote] = start_note

  def getEndNote: KeyboardNote = end_note.get
  def setEndNote(n: KeyboardNote): Unit = end_note.set(n)
  def getEndNoteProperty: SimpleObjectProperty[KeyboardNote] = end_note

  trait NoteStatus
  case object NoteActive extends NoteStatus
  case object NoteOff extends NoteStatus
  case object NoteSustained extends NoteStatus
  case class NoteDecaying(s: Double, lastUpdate: Long) extends NoteStatus

  case class RollNote(note: KeyboardNote, start: Long, end: Option[Long])
  case class RollSustain(start: Long, end: Option[Long])

  var activeNotes: Map[KeyboardNote, NoteStatus] = Map.empty
  var staticRollNotes: Map[KeyboardNote, List[RollNote]] = Map.empty
  var staticRollSustain: List[RollSustain] = List.empty

  var sustainActive = false

  val canvas = new Canvas(getWidth, getHeight)
  getChildren.add(canvas)

  canvas.widthProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  canvas.heightProperty().addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  piano_enabled.addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  piano_roll_enabled.addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  start_note.addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })
  end_note.addListener(new InvalidationListener() {
    override def invalidated(observable: Observable): Unit = draw()
  })

  val thread = new Thread(drawTask())
  thread.setDaemon(true)
  thread.start()

  def queueActiveNote(n: KeyboardNote): Unit = {
    activeNotes += (n -> NoteActive)
    staticRollNotes += (n -> staticRollNotes.getOrElse(n, List.empty[RollNote]).+:(RollNote(n, System.currentTimeMillis(), None)))
  }

  def dequeueActiveNote(n: KeyboardNote): Unit = {
    if(!sustainActive) {
      activeNotes += (n -> NoteDecaying(1.0f, System.currentTimeMillis()))
      staticRollNotes.get(n) match {
        case Some(rn) if rn.exists(_.end.isEmpty) =>
          staticRollNotes +=
            (n -> rn.filter(_.end.isDefined).+:(rn.find(_.end.isEmpty).get.copy(end = Some(System.currentTimeMillis()))))
        case _ =>
      }
    } else {
      activeNotes += (n -> NoteSustained)
    }
  }

  def sustainOn(): Unit = {
    sustainActive = true
    staticRollSustain = staticRollSustain.+:( RollSustain(System.currentTimeMillis(), None))
  }

  def sustainOff(): Unit = {
    sustainActive = false
    staticRollSustain =
      staticRollSustain.filter(_.end.isDefined) ++
      staticRollSustain.find(_.end.isEmpty).map(_.copy(end = Some(System.currentTimeMillis())))
    activeNotes =
      activeNotes
        .map {
          case (k, NoteSustained) =>
            staticRollNotes.get(k) match {
              case Some(rn) if rn.exists(_.end.isEmpty) =>
                staticRollNotes +=
                  (k -> rn.filter(_.end.isDefined).+:(rn.find(_.end.isEmpty).get.copy(end = Some(System.currentTimeMillis()))))
              case _ =>
            }
            (k, NoteDecaying(1.0f, System.currentTimeMillis()))
          case (k, v) => (k, v)
        }
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

    if(getStartNote != null && getEndNote != null) {
      val from =
        if (isLower(getStartNote.note)) {
          getStartNote
        } else {
          KeyboardNote.widthAbsoluteIndex(getStartNote.absoluteIndex() + 1)
        }

      val to =
        if (isLower(getEndNote.note)) {
          getEndNote
        } else {
          KeyboardNote.widthAbsoluteIndex(getEndNote.absoluteIndex() + 1)
        }

      gc.clearRect(0, 0, getWidth, getHeight)
      if (getPianoEnabled && getPianoRollEnabled) {
        drawRoll(gc, from, to, new Rectangle(0, 0, getWidth.toInt, (getHeight * 0.7).toInt))
        drawKeyboard(gc, from, to, new Rectangle(0, (getHeight * 0.7).toInt, getWidth.toInt, (getHeight * 0.3).toInt))
      } else if (getPianoEnabled) {
        drawKeyboard(gc, from, to, new Rectangle(0, 0, getWidth.toInt, getHeight.toInt))
      } else if (getPianoRollEnabled) {
        drawRoll(gc, from, to, new Rectangle(0, 0, getWidth.toInt, getHeight.toInt))
      }
    }
  }

  private def drawTask() = new Task[Unit]() {
    override def call(): Unit = {
      while(!isCancelled) {
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            draw()
          }
        })
        activeNotes =
          activeNotes
            .filterNot(_._2 == NoteOff)
            .map {
              case (k, NoteDecaying(s, lastUpdate)) if s > 0 => (k, NoteDecaying(Math.max(0, s - 0.15), lastUpdate))
              case (k, NoteDecaying(_, _)) => (k, NoteOff)
              case (k, v) => (k, v)
            }
        Thread.sleep(100)
      }
    }
  }

  private def drawKeyboard(gc: GraphicsContext, fromNote: KeyboardNote, toNote: KeyboardNote, r: Rectangle) = {
    val from = fromNote.absoluteIndex()
    val to = toNote.absoluteIndex()
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
        if(activeNotes.contains(n) && activeNotes(n) != NoteOff) {
          activeNotes(n) match {
            case NoteActive | NoteSustained =>
              gc.setFill(Color.LIGHTBLUE)
            case NoteDecaying(s, _) =>
              gc.setFill(Color.LIGHTBLUE.deriveColor(0, Math.max(s, 0.05), Math.min(Double.MaxValue, 1.0/s), 1.0))
          }
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
        if(activeNotes.contains(n) && activeNotes(n) != NoteOff ) {
          activeNotes(n) match {
            case NoteActive | NoteSustained =>
              gc.setFill(Color.LIGHTBLUE)
            case NoteDecaying(s, _) =>
              gc.setFill(Color.LIGHTBLUE.deriveColor(0, 1.0, s, 1.0))
          }
          gc.fillRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
          gc.strokeRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
        } else {
          gc.setFill(new Color(0.2, 0.2, 0.2, 1.0))
          gc.fillRect(r.x + (offset + 0.5)*lowerNoteWidth - upperNoteWidth/2, r.y, upperNoteWidth, upperNoteHeight)
        }
      }
  }

  private def drawRoll(gc: GraphicsContext, fromNote: KeyboardNote, toNote: KeyboardNote, r: Rectangle) = {
    val from = fromNote.absoluteIndex()
    val to = toNote.absoluteIndex()
    val notes = (from to to).map(KeyboardNote.widthAbsoluteIndex)
    val lowerNotes = notes.filter(n => isLower(n.note))
    val lowerNoteWidth = r.width / lowerNotes.size.toDouble
    val rollNoteWidth = 7.0 * lowerNoteWidth / 12.0
    val lowerNoteHeight = r.height

    gc.setStroke(Color.BLACK)
    gc.setFill(Color.WHITE)
    gc.fillRect(r.x, r.y, r.width, r.height)

    val currentTime = System.currentTimeMillis()
    val startReferenceTime =
      Math.min(
        currentTime,
        (List(Long.MinValue) ++ staticRollNotes.values.flatMap(_.map(x => x.end.getOrElse(x.start + 2000)))).max
      )
    val timeScaleFactor = 2/10000.0
    def timeScale(time: Long) = -1 + 2.0/(1.0 + Math.pow(1.0 + timeScaleFactor, -time))

    notes
      .zipWithIndex
      .foreach { case (n, offset) =>
        val width =
          if(offset == notes.size - 1) {
            lowerNoteWidth
          } else {
            rollNoteWidth
          }

        if(!lowerNotes.contains(n)) {
          gc.setFill(Color.LIGHTGRAY)
          gc.fillRect(r.x + rollNoteWidth*offset, r.y, rollNoteWidth, lowerNoteHeight)
        }
        gc.strokeRect(r.x + rollNoteWidth*offset, r.y, width, lowerNoteHeight)

        staticRollNotes.get(n) match {
          case Some(rn) =>
            rn
              .foreach { rollNote =>
                val noteStart = timeScale(startReferenceTime - rollNote.start)
                val noteEnd = timeScale(startReferenceTime - rollNote.end.getOrElse(startReferenceTime))

                gc.setFill(Color.LIGHTBLUE)
                gc.fillRect(
                  r.x + rollNoteWidth*offset,
                  r.y + r.height - r.height * noteStart,
                  rollNoteWidth,
                  r.height * Math.abs(noteEnd - noteStart)
                )
              }
          case _ =>
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