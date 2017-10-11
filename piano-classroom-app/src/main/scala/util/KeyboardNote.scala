package util

import javax.sound.midi.ShortMessage

import util.MusicNote.MusicNote

/**
  * Created by nesbu on 27/08/2017.
  */
object MusicNote extends Enumeration {
  type MusicNote = Value

  val C = Val(0, "C")
  val `C#-Db` = Val(1, "C#/Db")
  val D = Val(2, "D")
  val `D#-Eb` = Val(3, "D#/Eb")
  val E = Val(4, "E")
  val F = Val(5, "F")
  val `F#-Gb` = Val(6, "F#/Gb")
  val G = Val(7, "G")
  val `G#-Ab` = Val(8, "G#/Ab")
  val A = Val(9, "A")
  val `A#-Bb` = Val(10, "A#/Bb")
  val B = Val(11, "B")

  protected case class Val(index: Int, string: String) extends super.Val {
    override def toString(): String = {
      string
    }
  }

  implicit def valueToVal(x: Value) = x.asInstanceOf[Val]

  def withIndex(index: Int): Value = {
    values.find(_.index == index).getOrElse(throw new Exception("No Music Note with index ($index). Valid index should be between 0-11"))
  }
}

object KeyboardNote {
  def widthAbsoluteIndex(absIndex: Int) = {
    val octave = absIndex / 12
    val note = absIndex % 12
    KeyboardNote(MusicNote.withIndex(note), octave)
  }
}
case class KeyboardNote(note: MusicNote, index: Int) {
  def absoluteIndex() = 12*index + note.index
}
