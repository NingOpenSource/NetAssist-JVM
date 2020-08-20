package org.ning1994.net_assist

import javafx.animation.Interpolator
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.paint.Color
import javafx.util.StringConverter
import org.ning1994.net_assist.core.SocketProtocol
import tornadofx.*

class MainView : View("NetAssist", NetAssist.loadIcon()) {
    private val progressValue = SimpleDoubleProperty(0.0)
    private val socketProtocolList = SocketProtocol.values().asList()
    private val socketProtocol = SimpleObjectProperty<SocketProtocol>(socketProtocolList[0])
    private val ip = SimpleStringProperty("127.0.0.1")
    private val port = SimpleIntegerProperty(8888)


    override val root = vbox {
        borderpane {
            left {
                form {
                    fieldset("网络设置") {
                        field(orientation = Orientation.VERTICAL) {
                            style {
                                backgroundColor += Color.AQUA
                                paddingAll = 8
                            }
                            fieldset("协议类型") {
                                choicebox(socketProtocol, socketProtocolList) {
                                    this.converter = object : StringConverter<SocketProtocol>() {

                                        override fun toString(`object`: SocketProtocol?): String = `object`?.displayName
                                            ?: ""

                                        override fun fromString(string: String?): SocketProtocol =
                                            socketProtocolList.find { it.displayName == string }!!
                                    }
                                }
                            }
                            fieldset("远程IP地址") {
                                textfield(ip) {

                                }
                            }
                            fieldset("远程端口号") {
                                textfield(port) {

                                }
                            }
                        }

                    }

                }
            }
            right {

            }
            bottom {

            }
        }
        paddingLeft = 15
        paddingRight = 15
        paddingTop = 10
        paddingBottom = 10
        alignment = Pos.CENTER
        label("正在加载...") {
            paddingBottom = 5
        }
        progressbar(progressValue) {
            minWidth = 400.0
            progressProperty().animate(1, 0.5.seconds, Interpolator.LINEAR) {
                setOnFinished {
//                    close()
//                    MainView().openWindow()
                }
            }

        }
    }

}