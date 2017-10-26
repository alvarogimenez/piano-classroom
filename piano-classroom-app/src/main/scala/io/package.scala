import org.json4s._
import org.json4s.native.Serialization

package object io {
  def fromJson[T <: AnyRef : Manifest](source: String)(implicit f: Formats): T = {
    Serialization.read[T](source)
  }

  def toJson[T <: AnyRef: Manifest](t: T)(implicit f: Formats): String = {
    Serialization.writePretty(t)
  }
}
