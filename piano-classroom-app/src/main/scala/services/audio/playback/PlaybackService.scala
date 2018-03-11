package services.audio.playback

import javafx.application.Platform
import javafx.beans.property.{LongProperty, SimpleLongProperty}
import javafx.concurrent.Task
import javax.sound.midi.ShortMessage

class PlaybackService {
  private val HEARTBEAT_TIME = 100L
  private var task: Task[Unit] = _
  private val playback_time: LongProperty = new SimpleLongProperty()

  def getPlaybackTime: Long = playback_time.get
  def setPlaybackTime(playbackTime: Long): Unit = playback_time.set(playbackTime)
  def getPlaybackTimeProperty: LongProperty = playback_time

  def pause() = {
    stopThread()
  }

  def stop() = {
    stopThread()
    setPlaybackTime(0)
  }

  def play(events: List[PlaybackEvent]) = {
    val stages =
      events
        .groupBy(_.time)
        .filterKeys(_ >= getPlaybackTime)
        .toList
        .sortBy(_._1)
        .foldLeft(List.empty[PlaybackStage]) {
          case (accStages, (newTime, newEvents)) =>
            accStages :+ PlaybackStage(newTime, newEvents)
        }

    startThread(stages)
  }

  private def stopThread(): Unit = {
    if(task != null) {
      task.cancel()
    }
  }

  private def startThread(stages: List[PlaybackStage]): Unit = {
    task = playbackTask(stages)
    val thread = new Thread(task)
    thread.setDaemon(true)
    thread.start()
  }

  private def playbackTask(stages: List[PlaybackStage]) = new Task[Unit]() {
    var lastQueuedUpdatePlaybackTime: Long = 0

    private def queueUpdatePlaybackTime(time: Long) = {
      if(time - lastQueuedUpdatePlaybackTime > 100) {
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            setPlaybackTime(time)
          }
        })
        lastQueuedUpdatePlaybackTime = time
      }
    }

    def waitUntil(stageTime: Long, start: Long) = {
      val currentStageTime = System.currentTimeMillis() - start
      if(stageTime >= currentStageTime) {
        val timeGap = stageTime - currentStageTime
        ((0 until (timeGap/HEARTBEAT_TIME).toInt).map(_ => HEARTBEAT_TIME) :+ timeGap % HEARTBEAT_TIME).foreach { x =>
          queueUpdatePlaybackTime(System.currentTimeMillis() - start)
          Thread.sleep(x)
        }
      }
    }

    override def call(): Unit = {
      var i = 0
      val start = System.currentTimeMillis() - getPlaybackTime

      while(!isCancelled && i < stages.length) {
        val stage = stages(i)
        waitUntil(stage.time, start)
        stage.events.foreach {
          case e: PlaybackNoteOnEvent =>
            e.channel.queueMidiMessage(new ShortMessage(ShortMessage.NOTE_ON, e.note.absoluteIndex() + 12, 64))
          case e: PlaybackNoteOffEvent =>
            e.channel.queueMidiMessage(new ShortMessage(ShortMessage.NOTE_OFF, e.note.absoluteIndex() + 12, 64))
          case e: PlaybackSustainOnEvent =>
            e.channel.queueMidiMessage(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0x40, 127))
          case e: PlaybackSustainOffEvent =>
            e.channel.queueMidiMessage(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0x40, 0))
        }
        i += 1
      }
      while(!isCancelled) {
        queueUpdatePlaybackTime(System.currentTimeMillis() - start)
        Thread.sleep(HEARTBEAT_TIME)
      }
    }
  }
}
