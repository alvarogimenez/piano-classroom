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

)

case class GlobalIoConfiguration(
  `last-opened-file`: Option[String] = None
)

case class SessionContract(
  `audio-configuration`: Option[AsioConfiguration],
  `vst-configuration`: VstConfiguration,
  `global`: Option[GlobalConfiguration]
)
