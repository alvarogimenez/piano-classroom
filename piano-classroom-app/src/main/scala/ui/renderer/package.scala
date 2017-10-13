package ui

import javafx.scene.Node

package object renderer {
  trait RendererSlave { _ : Node =>
    def render(): Unit
  }
}
