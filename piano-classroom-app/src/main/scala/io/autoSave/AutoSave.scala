package io.autoSave

import java.io.{File, PrintWriter}
import javafx.application.Platform
import javafx.concurrent.Task

import context.Context
import io.toJson

class AutoSave {
  private var _task: Task[Unit] = _

  def stopThread(): Unit = {
    if(_task != null) {
      _task.cancel()
    }
  }

  def startThread(): Unit = {
    _task = task()
    val thread = new Thread(task)
    thread.setDaemon(true)
    thread.start()
  }

  private def task() = new Task[Unit]() {
    override def call(): Unit = {
      while(!isCancelled) {
        if(Context.projectSessionDirty.get()) {
          Platform.runLater(new Runnable() {
            def run(): Unit = {
              Context.projectSessionSaving.set(true)
            }
          })
          save()
          Platform.runLater(new Runnable() {
            def run(): Unit = {
              Context.projectSessionDirty.set(false)
              Context.projectSessionSaving.set(false)
            }
          })
        }
        Thread.sleep(2000)
      }
    }
  }

  private def save() = {
    import Context._

    Context.applicationSession.get().`global` match {
      case Some(global) =>
        global.`io` match {
          case Some(io) =>
            io.`last-opened-file` foreach { lastOpenedFile =>
              try {
                val w = new PrintWriter(new File(lastOpenedFile))
                w.write(toJson(Context.projectSession.get))
                w.close()
              } catch {
                case e: Exception =>
                  println(s"Error writing '$lastOpenedFile' file: '${e.getMessage}'")
              }
            }
          case None =>
        }
      case None =>
    }
  }
}
