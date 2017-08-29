import org.json4s._
import org.json4s.native.Serialization

package object io {
  implicit val formats = Serialization.formats(NoTypeHints)

  def fromJson[T <: AnyRef : Manifest](source: String): T = {
    Serialization.read[T](source)
  }

  def toJson[T <: AnyRef: Manifest](t: T): String = {
    Serialization.writePretty(t)
  }
}
