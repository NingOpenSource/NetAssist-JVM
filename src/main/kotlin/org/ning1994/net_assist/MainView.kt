package org.ning1994.net_assist

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.util.StringConverter
import org.ning1994.net_assist.core.ServiceStatus
import org.ning1994.net_assist.core.SocketProtocol
import org.ning1994.net_assist.widget.simpleTextfield
import tornadofx.*

class MainView : View("NetAssist", NetAssist.loadIcon()) {
    companion object {
        val viewModel = MainViewModel()
    }

    private val ip_lable_list = HashMap<SocketProtocol, String>().apply {
        put(SocketProtocol.tcpServer, "本地IP地址")
        put(SocketProtocol.tcpClient, "远程IP地址")
        put(SocketProtocol.udp, "本地IP地址")
    }
    private val port_lable_list = HashMap<SocketProtocol, String>().apply {
        put(SocketProtocol.tcpServer, "本地端口号")
        put(SocketProtocol.tcpClient, "远程端口号")
        put(SocketProtocol.udp, "本地端口号")
    }
    private val ip_lable = SimpleStringProperty(ip_lable_list[viewModel.socketProtocol.value])
    private val port_lable = SimpleStringProperty(port_lable_list[viewModel.socketProtocol.value])
    val inputText=SimpleStringProperty("")

    init {
        viewModel.port.addListener { _, _, newValue ->
//            receiveDataLogs.value += "$newValue\n"
        }
        viewModel.socketProtocol.addListener { _, _, newValue ->
            ip_lable.value = ip_lable_list[newValue]
            port_lable.value = port_lable_list[newValue]
        }
    }


    override val root = vbox {
        vgrow = Priority.ALWAYS
        minHeight = 600.0
        minWidth = 800.0
        borderpane {
            fitToParentSize()
            left {
                scrollpane(fitToHeight = true) {
                    minWidth = 340.0
                    form {
                        vgrow = Priority.ALWAYS
                        fieldset("网络设置") {
                            field(orientation = Orientation.VERTICAL) {
                                addClass(MainStyles.formBlockPanel)
                                label("协议类型")
                                choicebox(viewModel.socketProtocol, viewModel.socketProtocolList) {
                                    fitToParentWidth()
                                    converter = object : StringConverter<SocketProtocol>() {

                                        override fun toString(`object`: SocketProtocol?): String = `object`?.displayName
                                            ?: ""

                                        override fun fromString(string: String?): SocketProtocol =
                                            viewModel.socketProtocolList.find { it.displayName == string }!!
                                    }
                                    viewModel.serviceStatus.addListener { _, _, newValue ->
                                        isDisable = newValue != ServiceStatus.idle
                                    }
                                }
                                label(ip_lable)
                                textfield(viewModel.ip){
                                    viewModel.serviceStatus.addListener { _, _, newValue ->
                                        isDisable = newValue != ServiceStatus.idle
                                    }
                                }
                                label(port_lable)
                                simpleTextfield(viewModel.port){
                                    viewModel.serviceStatus.addListener { _, _, newValue ->
                                        isDisable = newValue != ServiceStatus.idle
                                    }
                                }
                                hbox(8) {
                                    button("开始监听") {
                                        viewModel.serviceStatus.addListener { _, _, newValue ->
                                            isDisable = newValue != ServiceStatus.idle
                                        }
                                        setOnAction {
                                            viewModel.start()
                                        }
                                    }
                                    button("断开"){
                                        isDisable=true
                                        viewModel.serviceStatus.addListener { _, _, newValue ->
                                            isDisable = newValue != ServiceStatus.running
                                        }
                                        setOnAction {
                                            viewModel.stop()
                                        }
                                    }
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
                                    fitToParentWidth()
                                    button("保存数据") {
                                        hgrow = Priority.ALWAYS
                                    }
                                    button("清空显示") {
                                        hgrow = Priority.ALWAYS
                                    }
                                }
                            }
                        }
                        fieldset("发送设置") {
                            field(orientation = Orientation.VERTICAL) {
                                addClass(MainStyles.formBlockPanel)
                                hbox(8) {
                                    alignment = Pos.CENTER_LEFT
                                    label("换行符")
                                    choicebox(viewModel.sendNewlineType, viewModel.sendNewlineTypes)
                                }
                                checkbox("载入文件")
                                checkbox("自动发送附加位")
                                checkbox("发送完自动清空")
                                checkbox("16进制发送")
                                hbox(8) {
                                    alignment = Pos.CENTER_LEFT
                                    checkbox("周期发送")
                                    simpleTextfield(viewModel.periodicSendTimeMS)
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
            }
            center {
                form {
                    alignment = Pos.BOTTOM_LEFT
                    fieldset("数据接收") {
                        fitToParentHeight()
                        field(orientation = Orientation.VERTICAL) {
                            fitToParentHeight()
                            addClass(MainStyles.formBlockPanel)
                            textarea(viewModel.receiveDataLogs) {
                                fitToParentHeight()
                                isEditable = false
                                viewModel.receiveDataLogs.addListener { _, _, _ ->
                                    selectEnd()
                                    deselect()
                                }
                            }
                        }
                    }
                    fieldset {
                        field("客户端：") {
                            addClass(MainStyles.formBlockPanel)
                            viewModel.socketProtocol.addListener { _, _, protocol ->
                                isVisible = protocol == SocketProtocol.tcpServer
                                isManaged = isVisible
                            }
                            isVisible = viewModel.socketProtocol.value == SocketProtocol.tcpServer
                            isManaged = isVisible
                            choicebox(viewModel.remoteClientInfo, viewModel.remoteClientInfoList) {
                                fitToParentWidth()
                            }
                        }
                        field("远程主机：") {
                            addClass(MainStyles.formBlockPanel)
                            viewModel.socketProtocol.addListener { _, _, protocol ->
                                isVisible = protocol == SocketProtocol.tcpClient
                                isManaged = isVisible
                            }
                            isVisible = viewModel.socketProtocol.value == SocketProtocol.tcpClient
                            isManaged = isVisible
                            choicebox(viewModel.remoteClientInfo, viewModel.remoteClientInfoList) {
                                fitToParentWidth()
                            }
                        }
                        field {
                            addClass(MainStyles.formBlockPanel)
                            viewModel.socketProtocol.addListener { _, _, protocol ->
                                isVisible = protocol == SocketProtocol.udp
                                isManaged = isVisible
                            }
                            isVisible = viewModel.socketProtocol.value == SocketProtocol.udp
                            isManaged = isVisible
                            label("远程IP："){
                                minWidth=50.0
                            }
                            textfield {
                                hgrow = Priority.ALWAYS
                            }
                            label("端口号："){
                                minWidth=50.0
                            }
                            textfield {
                                hgrow = Priority.ALWAYS
                            }
                        }
                    }
                    fieldset("数据发送") {
                        field {
                            addClass(MainStyles.formBlockPanel)
                            textarea(inputText)
                            button("数据发送") {
                                minWidth = 72.0
                                fitToParentHeight()
                                setOnAction {
                                    viewModel.send(inputText.value)
                                }
                            }
                        }
                    }
                }
            }
            bottom {
                hbox(8) {
                    addClass(MainStyles.formBlockPanel)
                    style {
                        backgroundRadius += box(0.0.px)
                    }
                    hbox {
                        hgrow = Priority.ALWAYS
                        alignment = Pos.CENTER_LEFT
                        label("状态：")
                        text("Ready")
                    }
                    hbox {
                        hgrow = Priority.ALWAYS
                        alignment = Pos.CENTER_LEFT
                        label("发送计数：")
                        text("0")
                    }
                    hbox {
                        hgrow = Priority.ALWAYS
                        alignment = Pos.CENTER_LEFT
                        label("接收计数：")
                        text("0")
                    }
                    button("清除") {
                    }
                }
            }
        }
    }
}