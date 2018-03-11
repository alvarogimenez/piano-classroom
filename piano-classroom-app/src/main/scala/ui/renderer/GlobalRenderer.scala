package ui.renderer

import javafx.application.Platform
import javafx.concurrent.Task

class GlobalRenderer {
  @volatile var slaves: List[RendererSlave] = List.empty[RendererSlave]

  private var task: Task[Unit] = _

  def addSlave(r: RendererSlave): Unit = slaves = slaves.+:(r)
  def stopThread(): Unit = {
    if(task != null) {
      task.cancel()
    }
  }
  def startThread(): Unit = {
    task = renderTask()
    val thread = new Thread(task)
    thread.setDaemon(true)
    thread.start()
  }

  private def renderTask() = new Task[Unit]() {
    override def call(): Unit = {
      while(!isCancelled) {
        Platform.runLater(new Runnable() {
          def run(): Unit = {
            slaves.foreach(_.render())
          }
        })
        Thread.sleep(100)
      }
    }
  }
}
