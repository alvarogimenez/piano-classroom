package ui.controller

import java.lang.Boolean
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXML
import javafx.scene.control.{Button, ToggleButton}

import context.Context
import ui.controller.mixer.MixerController
import ui.controller.monitor.MonitorController
import ui.controller.track._

import scala.collection.JavaConversions._

class MainStageController
  extends MenuBarController
    with ProjectSessionUpdating
    with MixerController
    with TrackSetController
    with MonitorController {

  @FXML var button_test: ToggleButton = _
  @FXML var button_refresh_rendering: Button = _
  @FXML var button_clear_all: Button = _
  @FXML var button_panic: Button = _

  def initialize(): Unit = {
    initializeMainStage()
    initializeMenuController(this)
    initializeMixerController(this)
    initializeTrackSetController(this)
    initializeMonitorController(this)
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

  private def initializeMainStage() = {
    button_test.selectedProperty().addListener(new ChangeListener[Boolean] {
      override def changed(observable: ObservableValue[_ <: Boolean], oldValue: Boolean, newValue: Boolean) = {
        println(newValue)
        if(newValue == true) {
          Context.midiService.startTestTask()
        } else {
          Context.midiService.stopTestTask()
        }
      }
    })

    button_refresh_rendering.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Refresh rendering...")
        Context.globalRenderer.stopThread()
        Thread.sleep(500)
        Context.globalRenderer.startThread()
      }
    })

    button_clear_all.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"Full data clear!")
        tracks
          .getChildren
          .foreach {
            case trackPanel: TrackPanel =>
              trackPanel.clear()
            case _ =>
          }
      }
    })

    button_panic.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        println(s"MIDI PANIC !!!")
        tracks
          .getChildren
          .foreach {
            case trackPanel: TrackPanel =>
              trackPanel.clear()
              trackPanel.panic()
            case _ =>
          }
      }
    })
  }
}
