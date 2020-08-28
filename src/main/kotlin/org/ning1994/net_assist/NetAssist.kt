package org.ning1994.net_assist

import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.ning1994.net_assist.core.DescriptionProperties
import tornadofx.App
import tornadofx.imageview
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.system.exitProcess

class NetAssist : App(MainView::class, MainStyles::class) {
    companion object {
        fun loadIcon(): Node {
            return VBox().apply {
                imageview(Image(NetAssist::class.java.getResourceAsStream("/ic_launcher.png")))
            }
        }

        private val descriptionProperties = Properties().apply {
            this.load(NetAssist::class.java.getResourceAsStream("/discription.properties"))
        }

        fun getDescription(description: DescriptionProperties): String {
            return descriptionProperties.getProperty(description.key)
        }

        val userDir = File(System.getProperty("user.dir"))
        private var workspaceDir: File? = null
        private var cacheDir: File? = null
        fun getWorkspaceDir(): File {
            if (workspaceDir == null) {
                workspaceDir = File(userDir, ".NetAssist-JVM").apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }
            }
            return workspaceDir!!
        }

        fun getCacheDir(): File {
            if (cacheDir == null) {
                cacheDir = File(getWorkspaceDir(), "cache").apply {
                    if (!exists()) {
                        mkdirs()
                    }
                }
            }
            return cacheDir!!
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