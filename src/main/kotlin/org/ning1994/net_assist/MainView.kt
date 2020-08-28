package org.ning1994.net_assist

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.util.StringConverter
import org.ning1994.net_assist.core.DescriptionProperties
import org.ning1994.net_assist.core.ServiceStatus
import org.ning1994.net_assist.core.SocketProtocol
import org.ning1994.net_assist.widget.simpleTextfield
import tornadofx.*
import java.awt.Desktop
import java.io.File
import java.net.URI

class MainView :
    View("NetAssist v${NetAssist.getDescription(DescriptionProperties.version_name)}", NetAssist.loadIcon()) {
    companion object {
        val viewModel = MainViewModel()
    }

    /**
     * ip标签名称
     */
    private val ip_lable = viewModel.socketProtocol.stringBinding {
        when (it) {
            SocketProtocol.tcpServer -> "本地IP地址"
            SocketProtocol.tcpClient -> "远程IP地址"
            SocketProtocol.udp -> "本地IP地址"
            else -> "ip_lable"
        }
    }

    /**
     * 端口标签名称
     */
    private val port_lable = viewModel.socketProtocol.stringBinding {
        when (it) {
            SocketProtocol.tcpServer -> "本地端口号"
            SocketProtocol.tcpClient -> "远程端口号"
            SocketProtocol.udp -> "本地端口号"
            else -> "port_lable"
        }
    }

    /**
     * 输入的待发送文本
     */
    private val inputText = SimpleStringProperty("")

    /**
     * 服务是否运行
     */
    private val enabledOnServiceRunning = viewModel.serviceStatus.booleanBinding {
        it == ServiceStatus.running
    }

    /**
     * 服务是否空闲
     */
    private val enabledOnServiceIdle = viewModel.serviceStatus.booleanBinding {
        it == ServiceStatus.idle
    }

    /**
     * 发送完成后是否清空
     */
    private val isClearTextAfterSend = SimpleBooleanProperty(false)

    /**
     * 当前循环发送的按钮
     */
    private val currentSendLooperButton = SimpleObjectProperty<Button>()

    /**
     * 文本循环发送标识
     */
    private val isTextSendLooperRunning = SimpleBooleanProperty(false)

    /**
     * 文件循环阿发送标识
     */
    private val isFileSendLooperRunning = SimpleBooleanProperty(false)

    override val root = borderpane {
        minHeight = 600.0
        minWidth = 800.0
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
                                enableWhen(enabledOnServiceIdle)
                            }
                            label(ip_lable)
                            textfield(viewModel.ip) {
                                enableWhen(enabledOnServiceIdle)
                            }
                            label(port_lable)
                            simpleTextfield(viewModel.port) {
                                enableWhen(enabledOnServiceIdle)
                            }
                            hbox(8) {
                                button("开始监听") {
                                    enableWhen(enabledOnServiceIdle)
                                    setOnAction {
                                        viewModel.start()
                                    }
                                }
                                button("断开") {
                                    enableWhen(enabledOnServiceRunning)
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
//                                checkbox("保存数据到文件")
//                                checkbox("自动换行显示")
                            checkbox("显示接收时间", viewModel.isPrintTimeInfo)
                            checkbox("16进制显示", viewModel.isPrintHexString)
                            checkbox("暂停显示", viewModel.isPrintPause)
                            hbox(8) {
                                fitToParentWidth()
                                button("保存数据") {
                                    hgrow = Priority.ALWAYS
                                    setOnAction {
                                        chooseFile(
                                            "请选择需要保存的文件",
                                            filters = arrayOf(
                                                FileChooser.ExtensionFilter(
                                                    "txt",
                                                    listOf("*.txt")
                                                )
                                            ),
                                            initialDirectory = NetAssist.userDir,
                                            mode = FileChooserMode.Save
                                        ).apply {
                                            if (isNotEmpty()) {
                                                val file = get(0)
                                                if (!file.exists()) {
                                                    file.createNewFile()
                                                }
                                                file.appendText(viewModel.receiveDataLogs.value)
                                                tooltip("保存成功：${file.absolutePath}") {
                                                    isAutoHide = true
                                                    show(currentWindow)
                                                }
                                            }
                                        }
                                    }
                                }
                                button("清空显示") {
                                    hgrow = Priority.ALWAYS
                                    setOnAction {
                                        viewModel.receiveDataLogs.value = ""
                                    }
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
                                choicebox(viewModel.lineSeparatorChar, viewModel.lineSeparatorCharList)
                            }
//                                checkbox("载入文件")
//                                checkbox("自动发送附加位")
                            checkbox("发送完自动清空", isClearTextAfterSend)
                            checkbox("16进制发送", viewModel.isSendHexString)
                            hbox(8) {
                                alignment = Pos.CENTER_LEFT
                                checkbox("周期发送", viewModel.isSendLooperEnable)
                                simpleTextfield(viewModel.sendLooperIntervalMS)
                                label("ms")
                            }
                            hbox(8) {
                                button(viewModel.isSendLooperEnable.and(isFileSendLooperRunning).stringBinding {
                                    if (it!!) "停止发送" else "发送文件"
                                }) {
                                    enableWhen(enabledOnServiceRunning.and(currentSendLooperButton.booleanBinding {
                                        if (it == null) true else it == this
                                    }))
                                    setOnAction {
                                        if (viewModel.isSendLooperEnable.value && isFileSendLooperRunning.value) {
                                            viewModel.sendLoopStop()
                                        } else {
                                            chooseFile(
                                                "请选择需要发送的文件（最大支持${Int.MAX_VALUE / 1024 / 1024}MB）",
                                                filters = arrayOf(/*FileChooser.ExtensionFilter("压缩包", listOf("*.zip"))*/),
                                                initialDirectory = File(System.getProperty("user.dir")),
                                                mode = FileChooserMode.Single
                                            ).apply {
                                                if (isNotEmpty()) {
                                                    val file = get(0)
                                                    if (file.length() <= Int.MAX_VALUE) {
                                                        if (viewModel.isSendLooperEnable.value && !isFileSendLooperRunning.value) {
                                                            isFileSendLooperRunning.value = true
                                                            currentSendLooperButton.value = this@button
                                                            viewModel.sendLoopStart(file) {
                                                                isFileSendLooperRunning.value = false
                                                                currentSendLooperButton.value = null
                                                            }
                                                        } else {
                                                            viewModel.send(file)
                                                        }
                                                    } else {
                                                        tooltip("文件过大!") {
                                                            isAutoHide = true
                                                            show(currentWindow)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                button("清空显示") {
                                    setOnAction {
                                        inputText.value = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        center {
            vbox(6.0) {
                padding = insets(10)
                label("数据接收")
                textarea(viewModel.receiveDataLogs) {
                    addClass(MainStyles.formBlockPanel)
                    fitToParentHeight()
                    isEditable = false
                    viewModel.receiveDataLogs.onChange {
                        selectEnd()
                        deselect()
                    }
                }
                hbox(8) {
                    addClass(MainStyles.formBlockPanel)
                    viewModel.socketProtocol.booleanBinding {
                        it == SocketProtocol.tcpServer
                    }.apply {
                        visibleWhen(this)
                        managedWhen(this)
                    }
                    alignment = Pos.BASELINE_CENTER
                    label("客户端：") {
                        minWidth = 72.0
                    }
                    choicebox(viewModel.selectedRemoteClientInfo, viewModel.remoteClientInfoList) {
                        fitToParentWidth()
                        enableWhen(
                            enabledOnServiceRunning.and(
                                isTextSendLooperRunning.or(
                                    isFileSendLooperRunning
                                ).not()
                            )
                        )
                    }
                }
                hbox(8) {
                    addClass(MainStyles.formBlockPanel)
                    alignment = Pos.BASELINE_CENTER
                    viewModel.socketProtocol.booleanBinding {
                        it == SocketProtocol.udp
                    }.apply {
                        visibleWhen(this)
                        managedWhen(this)
                    }
                    label("远程IP：") {
                        minWidth = 50.0
                    }
                    textfield(viewModel.ipUDP) {
                        hgrow = Priority.ALWAYS
                        enableWhen(
                            enabledOnServiceRunning.and(
                                isTextSendLooperRunning.or(
                                    isFileSendLooperRunning
                                ).not()
                            )
                        )
                    }
                    label("端口号：") {
                        minWidth = 50.0
                    }
                    simpleTextfield(viewModel.portUDP) {
                        hgrow = Priority.ALWAYS
                        enableWhen(
                            enabledOnServiceRunning.and(
                                isTextSendLooperRunning.or(
                                    isFileSendLooperRunning
                                ).not()
                            )
                        )
                    }
                }
                label("数据发送")
                hbox(8) {
                    addClass(MainStyles.formBlockPanel)
                    textarea(inputText) {
                        enableWhen(enabledOnServiceRunning.and(isTextSendLooperRunning.not()))
                        fitToParentWidth()
                        minHeight = 160.0
                    }
                    button(viewModel.isSendLooperEnable.and(isTextSendLooperRunning).stringBinding {
                        if (it!!) "停止发送" else "数据发送"
                    }) {
                        minWidth = 72.0
                        fitToParentHeight()
                        enableWhen(enabledOnServiceRunning.and(currentSendLooperButton.booleanBinding {
                            if (it == null) true else it == this
                        }))
                        setOnAction {
                            val onEnd = {
                                if (isClearTextAfterSend.value) {
                                    inputText.value = ""
                                }
                            }
                            if (viewModel.isSendLooperEnable.value) {
                                if (isTextSendLooperRunning.value) {
                                    viewModel.sendLoopStop()
                                } else {
                                    isTextSendLooperRunning.value = true
                                    currentSendLooperButton.value = this@button
                                    viewModel.sendLoopStart(inputText.value) {
                                        isTextSendLooperRunning.value = false
                                        currentSendLooperButton.value = null
                                        onEnd()
                                    }
                                }
                            } else {
                                viewModel.send(inputText.value, onEnd)
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
                    text(viewModel.serviceStatus.asString()) {
                        minWidth = 80.0
                    }
                }
                hbox {
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER_LEFT
                    label("发送计数：")
                    text(viewModel.totalSendDataSize) {
                        minWidth = 120.0
                    }
                }
                hbox {
                    hgrow = Priority.ALWAYS
                    alignment = Pos.CENTER_LEFT
                    label("接收计数：")
                    text(viewModel.totalReceiveDataSize) {
                        minWidth = 120.0
                    }
                }
                button("清除") {
                    setOnAction {
                        viewModel.clearTotalDataSize()
                    }
                }
                button("源代码") {
                    setOnAction {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().apply {
                                if (isSupported(Desktop.Action.BROWSE)) {
                                    browse(URI(NetAssist.getDescription(DescriptionProperties.repo_url)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}