package org.ning1994.net_assist

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import javafx.beans.property.*
import org.ning1994.net_assist.core.*
import org.ning1994.net_assist.utils.HexUtil
import org.ning1994.net_assist.utils.OSUtil
import tornadofx.observableListOf
import tornadofx.runLater
import java.io.File
import java.net.InetSocketAddress
import java.text.SimpleDateFormat


class MainViewModel {
    val socketProtocolList = SocketProtocol.values().asList()
    val socketProtocol = SimpleObjectProperty<SocketProtocol>(socketProtocolList[0])
    val ip = SimpleStringProperty("127.0.0.1")
    val port = SimpleIntegerProperty(8888)
    val ipUDP = SimpleStringProperty("127.0.0.1")
    val portUDP = SimpleIntegerProperty(8888)
    val lineSeparatorCharList = arrayListOf(
        LineSeparatorChar("\\r\\n", "\r\n"),
        LineSeparatorChar("\\n", "\n"),
        LineSeparatorChar("\\r", "\r"),
        LineSeparatorChar("\\n\\r", "\n\r"),
        LineSeparatorChar("禁止换行", "")
    )

    /**
     * 换行符
     */
    val lineSeparatorChar = SimpleObjectProperty(lineSeparatorCharList[0])

    /**
     * 日志缓存
     */
    val receiveDataLogs = SimpleStringProperty("")

    /**
     * 远程连接到本地服务端的客户端
     */
    val remoteClientInfoList = SimpleListProperty<Channel>(observableListOf(arrayListOf()))

    /**
     * 选中的远程客户端
     */
    val selectedRemoteClientInfo = SimpleObjectProperty<Channel>()

    /**
     * 是否打印时间信息
     */
    val isPrintTimeInfo = SimpleBooleanProperty(false)

    /**
     * 是否暂停打印
     */
    val isPrintPause = SimpleBooleanProperty(false)

    /**
     * 是否将接收到的数据打印为16进制
     */
    val isPrintHexString = SimpleBooleanProperty(false)

    /**
     * 是否将填写的内容解析为16进制数据发送
     */
    val isSendHexString = SimpleBooleanProperty(false)

    /**
     * 总共接收的数据总和（byte）
     */
    val totalReceiveDataSize = SimpleStringProperty("0")

    /**
     * 总共发送的数据总和（byte）
     */
    val totalSendDataSize = SimpleStringProperty("0")

    /**
     * 是否循环发送
     */
    val isSendLooperEnable = SimpleBooleanProperty(false)

    /**
     * 循环发送的间隔时间
     */
    val sendLooperIntervalMS = SimpleLongProperty(10)

    /**
     * 当前服务状态
     */
    val serviceStatus = SimpleObjectProperty<ServiceStatus>(ServiceStatus.idle)

    /**
     * 记录接收原始数据的临时文件
     */
    private val rawReceiveFile = createTempFile("NetAssist-jvm_", "_receive.log", NetAssist.getCacheDir()).apply {
        deleteOnExit()
    }

    /**
     * 记录发送原始数据的临时文件
     */
    private val rawSendFile = createTempFile("NetAssist-jvm_", "_send.log", NetAssist.getCacheDir()).apply {
        deleteOnExit()
    }

    /**
     * 单线程同步执行的任务调度器
     */
    private val ioExecutor = SingleThreadQueueExecutor()

    /**
     * 循环发送是否已经开始
     */
    @Volatile
    private var isSendLooperStarted = false

    /**
     * 用来接收进来的连接
     */
    var bossGroup: EventLoopGroup? = null

    /**
     * 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
     */
    var workerGroup: EventLoopGroup? = null

    /**
     * 服务启动时获取的channel
     */
    private var rootChannel: Channel? = null

    private fun initRootNettyChannel(ch: Channel?) {
        initNettyChannel(ch,
            activeCallback = {
                if (socketProtocol.value == SocketProtocol.tcpServer) {
                    log(ch!!, "服务器已启动")
                } else {
                    log(ch!!, "客户端已连接到服务器!")
                }
                if (serviceStatus.value == ServiceStatus.starting) {
                    serviceStatus.value = ServiceStatus.running
                }
            },
            inactiveCallback = {
                if (socketProtocol.value == SocketProtocol.tcpServer) {
                    log(ch!!, "服务器已关闭")
                } else {
                    log(ch!!, "客户端已关闭!")
                }
                if (serviceStatus.value == ServiceStatus.stopping
                    || serviceStatus.value == ServiceStatus.running
                ) {
                    serviceStatus.value = ServiceStatus.idle
                }
            })
    }

    private fun initNettyChannel(
        ch: Channel?,
        activeCallback: () -> Unit,
        inactiveCallback: () -> Unit,
        throwableCallback: (cause: Throwable) -> Unit = {}
    ) {
        rootChannel = ch
        ch?.pipeline()?.apply {
            addLast(ByteArrayEncoder())
            addLast(ByteArrayDecoder())
            addLast(object : SimpleChannelInboundHandler<ByteArray>() {
                override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteArray?) {
                    log(true, ctx?.channel()!!, msg!!)
                    rawReceiveFile.appendBytes(msg)
                    flushRawFile()
                }
            })
            addLast(object : SimpleChannelInboundHandler<DatagramPacket>() {
                override fun channelRead0(ctx: ChannelHandlerContext?, msg: DatagramPacket?) {
                    val byteBuf = msg?.content()
                    val bytes = ByteArray(byteBuf!!.readableBytes())
                    byteBuf.readBytes(bytes)
                    log(true, ch, bytes)
                    rawReceiveFile.appendBytes(bytes)
                    flushRawFile()
                }
            })
            addLast(object : LoggingHandler(LogLevel.DEBUG) {
                override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
                    @Suppress("DEPRECATION")
                    super.exceptionCaught(ctx, cause)
                    throwableCallback(cause!!)
                }

                override fun channelActive(ctx: ChannelHandlerContext?) {
                    super.channelActive(ctx)
                    activeCallback()
                }

                override fun channelInactive(ctx: ChannelHandlerContext?) {
                    super.channelInactive(ctx)
                    ch.also { channel ->
                        println("$channel is disconnected")
                        //检查是否有循环发送的任务，停止它
                        if (channel == findCurrentChannel() && isSendLooperEnable.value && isSendLooperStarted) {
                            sendLoopStop()
                        }
                    }
                    inactiveCallback()
                }
            })
        }
    }

    private fun log(isReceiver: Boolean, channel: Channel, message: ByteArray) {
        log(
            channel, if ((isPrintHexString.value && isReceiver)
                || (isSendHexString.value && !isReceiver)
            ) {
                HexUtil.encode(message)!!
            } else {
                String(message)
            }
        )
    }

    private val printDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private fun log(channel: Channel, message: String) {
        if (isPrintPause.value) return
        runLater {
            val timeInfo = if (isPrintTimeInfo.value) {
                "：${printDateFormat.format(System.currentTimeMillis())}"
            } else {
                ""
            }
            when (channel) {
                is DatagramChannel -> {
                    receiveDataLogs.value += "[UDP ${channel.localAddress()}]$timeInfo\n$message\n"
                }
                is NioServerSocketChannel -> {
                    receiveDataLogs.value += "[Local ${channel.localAddress()}]$timeInfo\n$message\n"
                }
                else -> {
                    receiveDataLogs.value += "[Remote ${channel.remoteAddress()}]$timeInfo\n$message\n"
                }
            }
        }
    }

    fun start() {
        if (serviceStatus.value != ServiceStatus.idle) {
            return
        }
        remoteClientInfoList.clear()
        println("正在启动...")
        serviceStatus.value = ServiceStatus.starting
        val channelFuture = when (socketProtocol.value!!) {
            SocketProtocol.tcpServer -> {
                bossGroup = NioEventLoopGroup()
                workerGroup = NioEventLoopGroup()
                ServerBootstrap().apply {
                    group(bossGroup, workerGroup)
//                    option(ChannelOption.SO_BACKLOG, 128)
                    childOption(ChannelOption.SO_KEEPALIVE, true)
                    channel(NioServerSocketChannel::class.java)
                    handler(object : ChannelInitializer<NioServerSocketChannel>() {
                        override fun initChannel(ch: NioServerSocketChannel?) {
                            rootChannel = ch
                            initRootNettyChannel(ch)
                        }
                    })
                    childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel?) {
                            initNettyChannel(ch, activeCallback = {
                                log(ch!!, "已连接!")
                                runLater {
                                    if (!remoteClientInfoList.contains(ch)) {
                                        remoteClientInfoList.add(ch)
                                    }
                                }
                            }, inactiveCallback = {
                                log(ch!!, "已断开!")
                                runLater {
                                    if (remoteClientInfoList.contains(ch)) {
                                        remoteClientInfoList.remove(ch)
                                    }
                                }
                            })
                        }
                    })
                }.bind(ip.value, port.value)
            }
            SocketProtocol.tcpClient -> {
                workerGroup = NioEventLoopGroup()
                Bootstrap().apply {
                    group(workerGroup)
                    channel(NioSocketChannel::class.java)
                    handler(object : ChannelInitializer<NioSocketChannel>() {
                        override fun initChannel(ch: NioSocketChannel?) {
                            rootChannel = ch
                            initRootNettyChannel(ch)
                        }
                    })
                }.connect(ip.value, port.value)
            }
            SocketProtocol.udp -> {
                workerGroup = NioEventLoopGroup()
                Bootstrap().apply {
                    group(workerGroup)
                    channel(NioDatagramChannel::class.java)
                    handler(object : ChannelInitializer<NioDatagramChannel>() {
                        override fun initChannel(ch: NioDatagramChannel?) {
                            rootChannel = ch
                            initRootNettyChannel(ch)
                        }
                    })
                }.bind(ip.value, port.value)
            }
        }
        channelFuture.addListener {
            println("start: isDone=${it.isDone}, isSuccess=${it.isSuccess}, isCancelled=${it.isCancelled}, isCancellable=${it.isCancellable}")
            if (it.isDone && it.isSuccess) {
                println("启动成功！...")
            } else {
                println("启动失败！...")
                log(channelFuture.channel(), "启动失败！...")
                serviceStatus.value = ServiceStatus.idle
            }
        }
    }

    fun stop() {
        if (serviceStatus.value == ServiceStatus.idle || serviceStatus.value == ServiceStatus.stopping) return
        serviceStatus.value = ServiceStatus.stopping
        rootChannel?.closeFuture()
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }

    fun send(data: String, onEnd: () -> Unit = {}) {
        _send(data, onEnd)
    }

    fun send(data: ByteArray, onEnd: () -> Unit = {}) {
        _send(data, onEnd)
    }

    fun send(data: File, onEnd: () -> Unit = {}) {
        _send(data, onEnd)
    }

    private fun findCurrentChannel(): Channel? {
        return if (socketProtocol.value == SocketProtocol.tcpServer) {
            selectedRemoteClientInfo.value
        } else {
            rootChannel
        }
    }

    private fun sendSync(data: ByteArray) {
        val channel = findCurrentChannel()
        if (socketProtocol.value == SocketProtocol.udp) {
            channel?.writeAndFlush(
                DatagramPacket(
                    Unpooled.wrappedBuffer(data),
                    InetSocketAddress(ipUDP.value, portUDP.value)
                )
            )
        } else {
            channel?.writeAndFlush(data)
        }?.sync().apply {
            rawSendFile.appendBytes(data)
            flushRawFile()
            log(false, rootChannel!!, data)
        }
    }

    private fun flushRawFile() {
        totalReceiveDataSize.value = rawReceiveFile.length().toString()
        totalSendDataSize.value = rawSendFile.length().toString()
    }

    fun clearTotalDataSize() {
        rawSendFile.writeText("")
        rawReceiveFile.writeText("")
        flushRawFile()
    }

    fun sendLoopStart(data: String, onEnd: () -> Unit) {
        _sendLoopStart(data, onEnd)
    }

    fun sendLoopStart(data: ByteArray, onEnd: () -> Unit) {
        _sendLoopStart(data, onEnd)
    }

    fun sendLoopStart(data: File, onEnd: () -> Unit) {
        _sendLoopStart(data, onEnd)
    }

    fun sendLoopStop() {
        if (isSendLooperEnable.value && isSendLooperStarted) {
            isSendLooperStarted = false
        }
    }

    private fun _sendLoopStart(data: Any, onEnd: () -> Unit) {
        if (isSendLooperEnable.value && !isSendLooperStarted) {
            isSendLooperStarted = true
            _send(data, onEnd)
        }
    }

    private fun _send(data: Any, onEnd: () -> Unit) {
        ioExecutor.execute {
            val byteArray = if (data is String) {
                //替换换行符
                val text = data.replace(
                    OSUtil.LINE_SEPARATOR_CHAR,
                    lineSeparatorChar.value.value
                )
                if (isSendHexString.value) {
                    HexUtil.decode(text)!!
                } else {
                    text.toByteArray()
                }
            } else if (data is ByteArray) {
                data
            } else if (data is File) {
                data.readBytes()
            } else {
                null
            }
            if (byteArray == null) {
                runLater(onEnd)
            } else {
                if (isSendLooperEnable.value) {
                    while (isSendLooperStarted) {
                        sendSync(byteArray)
                        Thread.sleep(sendLooperIntervalMS.value)
                    }
                } else {
                    sendSync(byteArray)
                }
                runLater(onEnd)
            }
        }
    }
}