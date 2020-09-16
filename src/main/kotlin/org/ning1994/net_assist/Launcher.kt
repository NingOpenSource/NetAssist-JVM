package org.ning1994.net_assist

import javafx.application.Application

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.launch(NetAssist::class.java, *args)
    }
}