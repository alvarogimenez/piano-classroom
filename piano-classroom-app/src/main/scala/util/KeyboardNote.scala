package util

import javax.sound.midi.ShortMessage

import util.MusicNote.MusicNote

object MusicNote extends Enumeration {
  type MusicNote = Value

  val C = Val(0, "C", "Do")
  val `C#-Db` = Val(1, "C#/Db", "Do#/Reb")
  val D = Val(2, "D", "Re")
  val `D#-Eb` = Val(3, "D#/Eb", "Re#/Mib")
  val E = Val(4, "E", "Mi")
  val F = Val(5, "F", "Fa")
  val `F#-Gb` = Val(6, "F#/Gb", "Fa#/Solb")
  val G = Val(7, "G", "Sol")
  val `G#-Ab` = Val(8, "G#/Ab", "Sol#/Lab")
  val A = Val(9, "A", "La")
  val `A#-Bb` = Val(10, "A#/Bb", "La#/Sib")
  val B = Val(11, "B", "Si")

  protected case class Val(index: Int, string: String, fixedDoString: String) extends super.Val {
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
