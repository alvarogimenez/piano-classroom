package ui.controller

import context.Context
import ui.controller.mixer.MixerController
import ui.controller.monitor.MonitorController
import ui.controller.track._


class MainStageController
  extends MenuBarController
    with MixerController
    with TrackSetController
    with MonitorController {

  def initialize(): Unit = {
    initializeMenuController()
    initializeMixerController()
    initializeTrackSetController()
    initializeMonitorController()
    Context.loadControllerDependantSettings(this)
  }
}
