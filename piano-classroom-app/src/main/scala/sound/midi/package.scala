package sound

package object midi {
  case class MidiInterfaceIdentifier(name: String) {
    override def toString(): String = name
  }

  case class MidiSubscriber(
    id: String,
    listener: MidiListener
  )
}
