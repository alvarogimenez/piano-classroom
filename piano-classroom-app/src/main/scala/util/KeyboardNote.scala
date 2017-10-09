package util

import javax.sound.midi.ShortMessage

import util.MusicNote.MusicNote

/**
  * Created by nesbu on 27/08/2017.
  */
object MusicNote extends Enumeration {
  type MusicNote = Value

  val C = Val(0)
  val `C#-Db` = Val(1)
  val D = Val(2)
  val `D#-Eb` = Val(3)
  val E = Val(4)
  val F = Val(5)
  val `F#-Gb` = Val(6)
  val G = Val(7)
  val `G#-Ab` = Val(8)
  val A = Val(9)
  val `A#-Bb` = Val(10)
  val B = Val(11)

  protected case class Val(index: Int) extends super.Val

  implicit def valueToPlanetVal(x: Value) = x.asInstanceOf[Val]

  def withIndex(index: Int): Value = {
    values.find(_.index == index).getOrElse(throw new Exception("No Music Note with index ($index). Valid index should be between 0-11"))
  }
}

object KeyboardNote {
  def widthAbsoluteIndex(absIndex: Int) = {
    val octave = (absIndex - 12) / 12
    val note = absIndex % 12
    KeyboardNote(MusicNote.withIndex(note), octave)
  }
}
case class KeyboardNote(note: MusicNote, index: Int) {
  def absoluteIndex() = 12*index + note.index
}
