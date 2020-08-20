package org.ning1994.net_assist

import javafx.animation.Interpolator
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.util.StringConverter
import org.ning1994.net_assist.core.SocketProtocol
import tornadofx.*

class MainView : View("NetAssist", NetAssist.loadIcon()) {
    private val progressValue = SimpleDoubleProperty(0.0)
    private val socketProtocolList = SocketProtocol.values().asList()
    private val socketProtocol = SimpleObjectProperty<SocketProtocol>(socketProtocolList[0])
    private val ip = SimpleStringProperty("127.0.0.1")
    private val port = SimpleIntegerProperty(8888)
    private val sendNewlineTypes = arrayListOf("\\n", "\\r\\n", "\\r", "无")
    private val sendNewlineType = SimpleStringProperty(sendNewlineTypes[0])

    override val root = vbox {
        borderpane {
            left {
                form {
                    fieldset("网络设置") {
                        field(orientation = Orientation.VERTICAL) {
                            addClass(MainStyles.formBlockPanel)
                            label("协议类型")
                            choicebox(socketProtocol, socketProtocolList) {
                                this.converter = object : StringConverter<SocketProtocol>() {

                                    override fun toString(`object`: SocketProtocol?): String = `object`?.displayName
                                        ?: ""

                                    override fun fromString(string: String?): SocketProtocol =
                                        socketProtocolList.find { it.displayName == string }!!
                                }
                            }
                            label("远程IP地址")
                            textfield(ip)
                            label("远程端口号")
                            textfield(port)
                            hbox(8) {
                                button("开始监听")
                                button("断开")
                            }
                        }
                    }
                    fieldset("接收设置") {
                        field(orientation = Orientation.VERTICAL) {
                            addClass(MainStyles.formBlockPanel)
                            checkbox("保存数据到文件")
                            checkbox("自动换行显示")
                            checkbox("显示接收时间")
                            checkbox("16进制显示")
                            checkbox("暂停显示")
                            hbox(8) {
                                button("保存数据")
                                button("清空显示")
                            }
                        }
                    }
                    fieldset("发送设置") {
                        field(orientation = Orientation.VERTICAL) {
                            addClass(MainStyles.formBlockPanel)
                            hbox(8) {
                                label("换行符")
                                choicebox(sendNewlineType, sendNewlineTypes)
                            }
                            checkbox("载入文件")
                            checkbox("自动发送附加位")
                            checkbox("发送完自动清空")
                            checkbox("16进制发送")
                            hbox(8) {
                                checkbox("周期发送")
                                textfield()
                                label("ms")
                            }
                            hbox(8) {
                                button("发送文件")
                                button("清空显示")
                            }
                        }
                    }
                }
            }
            center {
                form {
                    fieldset("数据接收") {
                        field(orientation = Orientation.VERTICAL) {
                            addClass(MainStyles.formBlockPanel)
                            textarea {  }

                        }
                    }
                    fieldset("数据发送") {
                        field {
                            addClass(MainStyles.formBlockPanel)
                            textarea {  }
                            button("数据发送")
                        }
                    }
                }
            }
            bottom {
                hbox(8) {
                    addClass(MainStyles.formBlockPanel)
                    label("正在加载...") {
                    }
                    progressbar(progressValue) {
                        minWidth = 400.0
                        progressProperty().animate(1, 0.5.seconds, Interpolator.LINEAR) {
                            setOnFinished {
                            }
                        }
                    }
                }
            }
        }
    }

}