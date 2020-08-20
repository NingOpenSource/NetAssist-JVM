package org.ning1994.net_assist

import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import tornadofx.App
import tornadofx.imageview

class NetAssist : App(MainView::class, MainStyles::class) {
    companion object {
        fun loadIcon(): Node {
            return VBox().apply {
                imageview(Image(NetAssist::class.java.getResourceAsStream("/ic_launcher.png")))
            }
        }
    }
}