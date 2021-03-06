package io.contracts

case class ProjectSessionContract(
  `version`: String,
  `save-state`: SaveState
)

case class SaveState(
  `tracks`: SaveTracks,
  `mixer`: SaveMixer,
  `monitor`: Option[GlobalMonitorConfiguration] = None,
  `recording`: Option[RecordingConfiguration] = None
)

/**
  * Recording
  */
case class RecordingConfiguration(
  `tracks`: List[RecordingTrackInfo]
)

case class RecordingTrackInfo(
  `channel-id`: String,
  `recording-enabled`: Boolean
)

/**
 * Tracks
 */
case class SaveTracks(
  `channel-info`: List[SaveChannelInfo],
  `channel-profiles`: List[SaveChannelProfile]
)

case class SaveChannelProfile(
  `name`: String,
  `color`: String,
  `channel-profiles`: List[SaveChannelProfileInfo]
)

case class SaveChannelProfileInfo(
  `id`: String,
  `midi-input`: Option[SaveChannelProfileMidiInput],
  `vst-input`: Option[SaveChannelProfileVstInput],
  `vst-properties`: Option[Map[String, Double]],
  `piano-enabled`: Boolean,
  `piano-roll-enabled`: Boolean,
  `piano-range-start`: SavePianoRange,
  `piano-range-end`: SavePianoRange
)

case class SaveChannelProfileMidiInput(
  `name`: String
)

case class SaveChannelProfileVstInput(
  `name`: String,
  `path`: String
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

case class GlobalMonitorKeyboardLayoutData(
  `note`: String,
  `note-index`: Int,
  `left`: Int,
  `right`: Int,
  `top`: Int,
  `bottom`: Int,
  `mask`: Option[String]
)

case class GlobalMonitorKeyboardLayout(
  `layout-data`: List[GlobalMonitorKeyboardLayoutData],
  `brightness-threshold`: Double,
  `smooth-average`: Double,
  `cut-y`: Int
)

case class GlobalMonitorCameraSettings(
  `source`: Option[String],
  `note-display`: Option[GlobalMonitorCameraNoteDisplaySettings],
  `sustain-active`: Option[Boolean],
  `highlighter-enabled`: Option[Boolean],
  `highlighter-subtractive`: Option[Boolean],
  `highlighter-subtractive-sensibility`: Option[Double],
  `keyboard-layout`: Option[GlobalMonitorKeyboardLayout]
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
