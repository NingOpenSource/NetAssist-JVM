package org.ning1994.net_assist

import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.App
import tornadofx.imageview
import kotlin.system.exitProcess

class NetAssist : App(MainView::class, MainStyles::class) {
    companion object {
        fun loadIcon(): Node {
            return VBox().apply {
                imageview(Image(NetAssist::class.java.getResourceAsStream("/ic_launcher.png")))
            }
        }
    }

    override fun start(stage: Stage) {
        super.start(stage)
        stage.setOnCloseRequest {
            stop()
        }
    }

    override fun stop() {
        super.stop()
        System.gc()
        exitProcess(0)
        @Suppress("UNREACHABLE_CODE")
        println("exit process...")
    }
}