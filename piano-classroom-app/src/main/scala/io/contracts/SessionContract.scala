package io.contracts

case class AsioChannelEnabled(
  `channel-number`: Int,
  enabled: Boolean
)

case class AsioChannelConfiguration(
  input: List[AsioChannelEnabled],
  output: List[AsioChannelEnabled]
)

case class AsioConfiguration(
  `driver-name`: String,
  `channel-configuration`: AsioChannelConfiguration
)

case class VstConfiguration(
  `vst-source-directories`: List[String]
)

case class GlobalConfiguration(
  `monitor`: Option[GlobalMonitorConfiguration] = None,
  `io`: Option[GlobalIoConfiguration] = None
)

case class GlobalMonitorConfiguration(
  `source-index`: Int,
  `fullscreen`: Boolean,
  `active-view`: Option[String],
  `camera-settings`: GlobalMonitorCameraSettings,
  `draw-board-settings`: GlobalMonitorDrawBoardSettings
)

case class GlobalMonitorCameraSettings(
  `source`: Option[String],
  `note-display`: Option[GlobalMonitorCameraNoteDisplaySettings]
)

case class GlobalMonitorCameraNoteDisplaySettings(
  `source-track-id`: Option[String],
  `display`: String
)

case class GlobalMonitorDrawBoardSettings(
  `pens`: Option[List[GlobalMonitorDrawBoardSettingsPen]],
  `selected-canvas-name`: Option[String],
  `canvas`: Option[List[GlobalMonitorDrawBoardSettingsCanvas]]
)

case class GlobalMonitorDrawBoardSettingsPen(
  `size`: Int,
  `r`: Int,
  `g`: Int,
  `b`: Int
)

case class GlobalMonitorDrawBoardSettingsCanvas(
  `name`: String,
  `aspect-ratio`: Double,
  `shapes`: List[GlobalMonitorDrawBoardSettingsCanvasShape]
)

sealed trait GlobalMonitorDrawBoardSettingsCanvasShape {
  val `type`: String
}
case class GlobalMonitorDrawBoardSettingsCanvasLine(
  `type`: String = "Line",
  `id`: String,
  `size`: Double,
  `color`: GlobalMonitorDrawBoardSettingsCanvasColor,
  `path`: String
)  extends GlobalMonitorDrawBoardSettingsCanvasShape

case class GlobalMonitorDrawBoardSettingsCanvasColor(
  `r`: Int,
  `g`: Int,
  `b`: Int
)

case class GlobalIoConfiguration(
  `last-opened-file`: Option[String] = None
)

case class SessionContract(
  `audio-configuration`: Option[AsioConfiguration],
  `vst-configuration`: VstConfiguration,
  `global`: Option[GlobalConfiguration]
)
