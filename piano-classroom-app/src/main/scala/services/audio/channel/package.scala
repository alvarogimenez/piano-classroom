package services.audio

import javafx.beans.property.{SimpleStringProperty, StringProperty}

package object channel {
  trait Channel {
    private val name_property: StringProperty = new SimpleStringProperty()
    private val id_property: StringProperty = new SimpleStringProperty()

    def getId: String = id_property.get
    def setId(id: String) = id_property.set(id)
    def getIdProperty: StringProperty = id_property

    def getName: String = name_property.get
    def setName(name: String) = name_property.set(name)
    def getNameProperty: StringProperty = name_property

    def pull(sampleRate: Double, bufferSize: Int): Array[Float]
    def close()

    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case x: Channel => x.getId == getId
        case _ => false
      }
    }
  }
}
