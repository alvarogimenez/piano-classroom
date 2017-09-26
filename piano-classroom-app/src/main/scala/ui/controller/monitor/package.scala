package ui.controller


package object monitor {
  case class WebCamSource(
    name: String,
    index: Int
  ) {
    override def toString(): String = name
  }
}
