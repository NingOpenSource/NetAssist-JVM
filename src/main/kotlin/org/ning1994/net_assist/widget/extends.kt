package org.ning1994.net_assist.widget

import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.util.StringConverter
import tornadofx.bind
import tornadofx.textfield


fun EventTarget.simpleTextfield(property: ObservableValue<Number>, op: TextField.() -> Unit = {}) = textfield(property) {
    textFormatter = TextFormatter(object : StringConverter<Number>() {
        override fun toString(`object`: Number?): String {
            if (`object` == null) return ""
            return `object`.toString()
        }

        override fun fromString(string: String?): Number {
            if (string.isNullOrEmpty()) {
                return 0
            }
            try {
                return string.toInt()
            } catch (t: Throwable) {
                t.printStackTrace()
                return property.value
            }
        }
    }, property.value)
    op(this)
}

