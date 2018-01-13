package io.contracts

case class ProjectSessionContract(
  `version`: String,
  `save-state`: SaveState
)

case class SaveState(
  `tracks`: SaveTracks,
  `mixer`: SaveMixer,
  `monitor`: Option[GlobalMonitorConfiguration] = None
)

/**
 * Tracks
 */
case class SaveTracks(
  `channel-info`: List[SaveChannelInfo]
)

case class SaveChannelInfo(
  `id`: String,
  `name`: String,
  `midi-input`: Option[String],
  `vst-i`: Option[String],
  `vst-properties`: Option[Map[String, Double]],
  `piano-enabled`: Boolean,
  `piano-roll-enabled`: Boolean,
  `piano-range-start`: SavePianoRange,
  `piano-range-end`: SavePianoRange
)

case class SavePianoRange(
  `note`: String,
  index: Int
)

/**
  * Mixer
  */
case class SaveMixer(
  `bus-info`: List[SaveBusInfo],
  `mixer-profiles`: List[SaveMixerProfile]
)

case class SaveBusInfo(
  `bus`: Int,
  `master-level`: Double,
  `bus-mix`: List[SaveBusMix],
  `bus-profiles`: List[SaveBusMixProfile]
)

case class SaveBusMix(
  `channel-id`: String,
  `level`: Option[Double]
)

case class SaveMixerProfile(
  `name`: String,
  `color`: String,
  `bus-profiles`: List[SaveBusProfile]
)

case class SaveBusProfile(
  `bus`: Int,
  `bus-level`: Double,
  `bus-mixes`: List[SaveBusChannelMixProfile]
)

case class SaveBusMixProfile(
  `name`: String,
  `color`: String,
  `bus-level`: Double,
  `bus-mixes`: List[SaveBusChannelMixProfile]
)

case class SaveBusChannelMixProfile(
  `channel`: String,
  `mix`: Double,
  `active`: Boolean,
  `solo`: Boolean
)

/**
  * Monitor
  */
case class GlobalMonitorConfiguration(
  `source-index`: Int,
  `fullscreen`: Boolean,
  `active-view`: Option[String],
  `camera-settings`: GlobalMonitorCameraSettings,
  `draw-board-settings`: GlobalMonitorDrawBoardSettings
)

case class GlobalMonitorCameraSettings(
  `source`: Option[String],
  `note-display`: Option[GlobalMonitorCameraNoteDisplaySettings],
  `sustain-active`: Option[Boolean]
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
