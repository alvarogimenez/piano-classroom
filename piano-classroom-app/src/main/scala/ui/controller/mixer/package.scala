package ui.controller

import javafx.scene.paint.Color


package object mixer {
  case class MixerGlobalProfile(
    name: String,
    color: Color,
    busMixes: List[MixerBusMixProfile]
  )

  case class MixerBusProfile(
    bus: Int,
    name: String,
    color: Color,
    mixes: MixerBusMixProfile
  )

  case class MixerBusMixProfile(
    bus: Int,
    mixes: List[MixerChannelMixProfile]
  )

  case class MixerChannelMixProfile(
    channel: String,
    mix: Float,
    active: Boolean,
    solo: Boolean
  )
}
