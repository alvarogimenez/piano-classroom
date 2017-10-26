package io

import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.{Formats, JValue, Serializer, TypeInfo}
import org.json4s._

package object contracts {
  def initializeEmptySesionContract(): SessionContract = {
    SessionContract(
      `audio-configuration` = None,
      `vst-configuration` = VstConfiguration(
        `vst-source-directories` = List(".")
      ),
      `global` = None
    )
  }

  class GlobalMonitorDrawBoardSettingsCanvasShapeSerializer extends Serializer[GlobalMonitorDrawBoardSettingsCanvasShape] {
    private val Class = classOf[GlobalMonitorDrawBoardSettingsCanvasShape]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), GlobalMonitorDrawBoardSettingsCanvasShape] = {
      case (TypeInfo(Class, _), json) =>
        json match {
          case json @ JObject(JField("type", JString("Line")) :: _) =>
            json.extract[GlobalMonitorDrawBoardSettingsCanvasLine]
          case _ =>
            throw new Exception("Unrecognized GlobalMonitorDrawBoardSettingsCanvasShape")
        }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = Map()
  }
}
