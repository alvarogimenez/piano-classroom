package services

package object midi {
  case class MidiInterfaceIdentifier(name: String) {
    override def toString(): String = name
  }

  case class MidiVstSource(name: String, path: String) {
    override def toString(): String = name
  }

  case class MidiSubscriber(
    id: String,
    listener: MidiListener
  )
}
