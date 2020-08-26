package org.ning1994.net_assist

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DatagramPacketDecoder
import io.netty.handler.codec.DatagramPacketEncoder
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import javafx.beans.property.*
import org.ning1994.net_assist.core.LineSeparatorChar
import org.ning1994.net_assist.core.ServiceStatus
import org.ning1994.net_assist.core.SingleThreadQueueExecutor
import org.ning1994.net_assist.core.SocketProtocol
import org.ning1994.net_assist.utils.HexUtil
import tornadofx.observableListOf
import tornadofx.runLater
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
    val lineSeparatorChar = SimpleObjectProperty(lineSeparatorCharList[0])
    val receiveDataLogs = SimpleStringProperty("")
    val periodicSendTimeMS = SimpleIntegerProperty(10)
    val remoteClientInfoList = SimpleListProperty<Channel>(observableListOf(arrayListOf()))
    val remoteClientInfo = SimpleObjectProperty<Channel>()
    val isPrintTimeInfo = SimpleBooleanProperty(false)
    val isPrintPause = SimpleBooleanProperty(false)
    val isPrintHexString = SimpleBooleanProperty(false)
    val isSendHexString = SimpleBooleanProperty(false)

    val serviceStatus = SimpleObjectProperty<ServiceStatus>(ServiceStatus.idle)

    //    private val ioExecutor = SingleThreadQueueExecutor()
    /**
     * 用来接收进来的连接
     */
    var bossGroup: EventLoopGroup? = null

    /**
     * 用来处理已经被接收的连接，一旦bossGroup接收到连接，就会把连接信息注册到workerGroup上
     */
    var workerGroup: EventLoopGroup? = null
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
            addLast(DatagramPacketDecoder(ByteArrayDecoder()))
            addLast(DatagramPacketEncoder(ByteArrayEncoder()))
            addLast(object : SimpleChannelInboundHandler<DatagramPacket>() {
                override fun channelRead0(ctx: ChannelHandlerContext?, msg: DatagramPacket?) {
                    log(ch, msg?.content()?.array()!!)
                }
            })
            addLast(ByteArrayEncoder())
            addLast(ByteArrayDecoder())
            addLast(object : SimpleChannelInboundHandler<ByteArray>() {
                override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteArray?) {
                    log(ch, msg!!)
                }
            })
            addLast(object : LoggingHandler(LogLevel.DEBUG) {
                override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
                    super.exceptionCaught(ctx, cause)
                    throwableCallback(cause!!)
                }

                override fun channelActive(ctx: ChannelHandlerContext?) {
                    super.channelActive(ctx)
                    activeCallback()
                }

                override fun channelInactive(ctx: ChannelHandlerContext?) {
                    super.channelInactive(ctx)
                    inactiveCallback()
                }
            })
        }
    }

    private fun log(channel: Channel, message: ByteArray) {
        log(
            channel, if (isPrintHexString.value) {
                HexUtil.encode(message)!!
            } else {
                String(message)
            }
        )
    }

    private val printDateFormate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private fun log(channel: Channel, message: String) {
        if (isPrintPause.value) return
        runLater {
            val timeInfo = if (isPrintTimeInfo.value) {
                "：${printDateFormate.format(System.currentTimeMillis())}"
            } else {
                ""
            }
            if (channel is NioServerSocketChannel) {
                receiveDataLogs.value += "[Local ${channel.localAddress()}]$timeInfo\n$message\n"
            } else {
                receiveDataLogs.value += "[Remote ${channel.remoteAddress()}]$timeInfo\n$message\n"
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
                }.connect(ip.value, port.value)
            }
        }
        channelFuture.addListener {
            println("start: isDone=${it.isDone}, isSuccess=${it.isSuccess}, isCancelled=${it.isCancelled}, isCancellable=${it.isCancellable}")
        }
        println("启动成功！...")
    }

    fun stop() {
        if (serviceStatus.value == ServiceStatus.idle || serviceStatus.value == ServiceStatus.stopping) return
        serviceStatus.value = ServiceStatus.stopping
        rootChannel?.closeFuture()
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }


    fun send(data: String): ChannelFuture? {
        if (isSendHexString.value) {
            return send(HexUtil.decode(data)!!)
        }
        return send(data.toByteArray())
    }

    fun send(data: ByteArray): ChannelFuture? {
        val channel =
            if (socketProtocol.value == SocketProtocol.tcpServer) {
                remoteClientInfo.value
            } else {
                rootChannel
            }
        return if (socketProtocol.value == SocketProtocol.udp) {
            channel?.writeAndFlush(
                DatagramPacket(
                    Unpooled.wrappedBuffer(data),
                    InetSocketAddress(ipUDP.value, portUDP.value)
                )
            )
        } else {
            channel?.writeAndFlush(data)
        }?.apply {
            log(rootChannel!!, data)
        }
    }
}