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
  `io`: Option[GlobalIoConfiguration] = None
)

case class GlobalIoConfiguration(
  `last-opened-file`: Option[String] = None
)

case class ApplicationSessionContract(
  `audio-configuration`: Option[AsioConfiguration],
  `vst-configuration`: VstConfiguration,
  `global`: Option[GlobalConfiguration]
)
