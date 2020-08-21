package org.ning1994.net_assist

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class MainStyles : Stylesheet() {
    companion object {
        val heading by cssclass()
        val formBlockPanel by cssclass()
    }

    init {
        label and heading {
            padding = box(10.px)
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }
        text {
            fontSize = 12.px
        }
        title {
            fontSize = 12.px
        }
        choiceBox {
            fontSize = 12.px
        }
        formBlockPanel {
            backgroundColor += Color.web("#e8e8e8")
            backgroundRadius += box(4.0.px)
            padding = box(12.0.px)
        }
    }
}