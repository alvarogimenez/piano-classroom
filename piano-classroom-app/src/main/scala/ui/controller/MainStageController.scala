package ui.controller

import context.Context
import ui.controller.global.{FooterController, MenuBarController, ProjectSessionUpdating}
import ui.controller.mixer.MixerController
import ui.controller.monitor.MonitorController
import ui.controller.track._

class MainStageController
  extends MenuBarController
    with ProjectSessionUpdating
    with MixerController
    with TrackSetController
    with MonitorController
    with FooterController {

  def initialize(): Unit = {
    initializeMenuController(this)
    initializeMixerController(this)
    initializeTrackSetController(this)
    initializeMonitorController(this)
    initializeFooterController(this)
    Context.loadControllerDependantSettings(this)
  }

  override def updateProjectSession() = {
    context.writeProjectSessionSettings(
      Context.projectSession.copy(
        `save-state` =
          Context.projectSession.`save-state`.copy(
            `tracks`= getTrackSession(),
            `monitor`= Some(getMonitorSession()),
            `mixer`= getMixerSession()
          )
      )
    )
  }
}
