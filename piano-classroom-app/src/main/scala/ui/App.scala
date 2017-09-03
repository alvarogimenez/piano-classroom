package ui

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.{Stage, WindowEvent}

import context.Context


class App extends Application {
  override def start(primaryStage: Stage): Unit = {
    Context.primaryStage = primaryStage

    val loader = new FXMLLoader()
    loader.setLocation(Thread.currentThread.getContextClassLoader.getResource("ui/view/MainStage.fxml"))
    Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)
    val rootLayout = loader.load().asInstanceOf[BorderPane]

    primaryStage.setOnCloseRequest(new EventHandler[WindowEvent]() {
      override def handle(event: WindowEvent): Unit = {
        Context.asioController.unloadStop()
        Context.midiController.detach()
      }
    })
    val scene = new Scene(rootLayout, 800, 600)
    scene.getStylesheets.add("ui/css/stylesheet.css")
    primaryStage.setMinHeight(rootLayout.getMinHeight)
    primaryStage.setMinWidth(rootLayout.getMinWidth)
    primaryStage.setScene(scene)
    primaryStage.setTitle("Piano Classroom v1.0.0")
    primaryStage.show()
  }
}
