package ui.controller

import javafx.scene.paint.Color


package object mixer {
  case class MixerProfile(
    name: String,
    color: Color,
    busMixes: List[BusProfile]
  )

  case class BusProfile(
    bus: Int,
    busLevel: Float,
    busMixes: List[BusChannelMixProfile]
  )

  case class BusMixProfile(
    name: String,
    color: Color,
    busLevel: Float,
    busMixes: List[BusChannelMixProfile]
  )

  case class BusChannelMixProfile(
    channel: String,
    mix: Float,
    active: Boolean,
    solo: Boolean
  )
}
