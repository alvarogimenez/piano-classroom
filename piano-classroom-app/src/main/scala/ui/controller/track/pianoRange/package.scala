package ui.controller.track

import util.KeyboardNote

/**
  * Created by nesbu on 11/10/2017.
  */
package object pianoRange {
  final val PIANO_RANGE_MODAL_ACCEPT = 1
  final val PIANO_RANGE_MODAL_CANCEL = 0

  trait TrackSubscriber {
    def noteOn(kn: KeyboardNote)
    def noteOff(kn: KeyboardNote)
    def sustainOn()
    def sustainOff()
  }
}
