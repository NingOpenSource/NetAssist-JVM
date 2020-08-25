package org.ning1994.net_assist

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import org.ning1994.net_assist.core.ServiceStatus
import org.ning1994.net_assist.core.SingleThreadQueueExecutor
import org.ning1994.net_assist.core.SocketProtocol
import java.util.*


class MainViewModel {
    val socketProtocolList = SocketProtocol.values().asList()
    val socketProtocol = SimpleObjectProperty<SocketProtocol>(socketProtocolList[0])
    val ip = SimpleStringProperty("127.0.0.1")
    val port = SimpleIntegerProperty(8888)
    val sendNewlineTypes = arrayListOf("\\n", "\\r\\n", "\\r", "无")
    val sendNewlineType = SimpleStringProperty(sendNewlineTypes[0])
    val receiveDataLogs = SimpleStringProperty("")
    val periodicSendTimeMS = SimpleIntegerProperty(10)
    val remoteClientInfoList = SimpleListProperty<String>()
    val remoteClientInfo = SimpleStringProperty()

    val serviceStatus = SimpleObjectProperty<ServiceStatus>(ServiceStatus.idle)

    private val ioExecutor = SingleThreadQueueExecutor()
    private val nioEventLoopGroup = NioEventLoopGroup(1)
    private val queueExecutor = SingleThreadQueueExecutor()
    private val channelList = Vector<Channel>()
    private var rootChannel: Channel? = null

    private fun initNettyChannel(ch: Channel?) {
        rootChannel = ch
        ch?.pipeline()?.apply {
            addLast(StringEncoder())
            addLast(ByteArrayEncoder())
//            addLast(ByteArrayDecoder())
            addLast(StringDecoder())
            addLast(object : SimpleChannelInboundHandler<String>() {
                override fun channelRead0(
                    ctx: ChannelHandlerContext?,
                    msg: String?
                ) {
                    remoteLog(ctx!!.channel(), msg!!)
                }

                override fun channelActive(ctx: ChannelHandlerContext?) {
                    super.channelActive(ctx)
                    if (socketProtocol.value == SocketProtocol.tcpServer) {
                        if (ctx?.channel() != null) {
                            log("已连接到服务器:${ctx.channel()}：TCP服务器启动完成")
                        } else {
                            localLog("TCP服务器启动完成")
                        }
                    } else {
                        log("已连接到服务器!")
                    }
                    if (serviceStatus.value == ServiceStatus.starting) {
                        serviceStatus.value = ServiceStatus.running
                    }
                }

                override fun channelInactive(ctx: ChannelHandlerContext?) {
                    super.channelInactive(ctx)
                    remoteLog(ctx!!.channel(), "服务端断开连接...")
                    if (serviceStatus.value == ServiceStatus.stopping
                        || serviceStatus.value == ServiceStatus.running
                    ) {
                        serviceStatus.value = ServiceStatus.idle
                    }
                }
            })
        }
    }

    fun localLog(message: String) {
        receiveDataLogs.value += "[Local /${ip.value}:${port.value}]\n$message\n"
    }

    fun log(message: String) {
        receiveDataLogs.value += "$message\n"
    }

    private fun remoteLog(channel: Channel, message: String) {
        receiveDataLogs.value += "[Remote ${channel.remoteAddress()}]\n$message\n"
    }
//    private fun log(socketAddress: SocketAddress, message: String){
//        receiveDataLogs.value += "[Remote IP ${socketAddress.} Port: 55595 ]${channel.remoteAddress()}\n$message\n"
//
//    }

    fun start() {
        if (serviceStatus.value != ServiceStatus.idle) {
            return
        }
        println("正在启动...")
        serviceStatus.value = ServiceStatus.starting
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        val bootstrap = Bootstrap()
            .group(nioEventLoopGroup)
        when (socketProtocol.value) {
            SocketProtocol.tcpServer -> {
                bootstrap.channel(NioServerSocketChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioServerSocketChannel>() {
                    override fun initChannel(ch: NioServerSocketChannel?) {
                        initNettyChannel(ch)
                    }
                })
                bootstrap.bind(ip.value, port.value)
            }
            SocketProtocol.tcpClient -> {
                bootstrap.channel(NioSocketChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioSocketChannel>() {
                    override fun initChannel(ch: NioSocketChannel?) {
                        initNettyChannel(ch)
                    }
                })
                bootstrap.connect(ip.value, port.value)
            }
            SocketProtocol.udp -> {
                bootstrap.channel(NioDatagramChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioDatagramChannel>() {
                    override fun initChannel(ch: NioDatagramChannel?) {
                        initNettyChannel(ch)
                    }
                })
                bootstrap.connect(ip.value, port.value)
            }
        }
        println("启动成功！...")
    }

    fun stop() {
        if (serviceStatus.value == ServiceStatus.idle || serviceStatus.value == ServiceStatus.stopping) return
        serviceStatus.value = ServiceStatus.stopping
        rootChannel?.close()
    }

    fun send(data: ByteArray) {
        rootChannel?.apply {
            ioExecutor.execute {
                writeAndFlush(data)
            }
        }
    }

    fun send(data: String) {
        rootChannel?.apply {
            ioExecutor.execute {
                writeAndFlush(data)
                localLog(data)
            }
        }
    }
}