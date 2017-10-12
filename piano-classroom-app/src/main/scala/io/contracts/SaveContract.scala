package io.contracts

case class SaveContract(
  `version`: String,
  `save-state`: SaveState
)

case class SaveState(
  `tracks`: SaveTracks,
  `mixer`: SaveMixer
)

case class SaveTracks(
  `channel-info`: List[SaveChannelInfo]
)

case class SaveMixer(
  `bus-info`: List[SaveBusInfo]
)

case class SaveChannelInfo(
  `id`: String,
  `name`: String,
  `midi-input`: String,
  `vst-i`: String,
  `piano-enabled`: Boolean,
  `piano-roll-enabled`: Boolean,
  `piano-range-start`: SavePianoRange,
  `piano-range-end`: SavePianoRange
)

case class SavePianoRange(
  `note`: String,
  index: Int
)

case class SaveBusInfo(
  `bus`: Int,
  `master-level`: Double,
  `bus-mix`: List[SaveBusMix]
)

case class SaveBusMix(
  `channel-id`: String,
  `level`: Double
)


