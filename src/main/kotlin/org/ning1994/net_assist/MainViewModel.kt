package org.ning1994.net_assist

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
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
    private var bootstrap = Bootstrap()
        .group(nioEventLoopGroup)

    private fun initNettyChannel(ch: Channel?) {
        rootChannel = ch
        ch?.pipeline()?.apply {
            addLast(object : MessageToByteEncoder<String>() {
                override fun encode(
                    ctx: ChannelHandlerContext?,
                    msg: String?,
                    out: ByteBuf?
                ) {
                    out?.writeBytes(msg!!.toByteArray())
                }
            })
            addLast(object : ByteToMessageDecoder() {
                override fun decode(
                    ctx: ChannelHandlerContext?,
                    `in`: ByteBuf?,
                    out: MutableList<Any>?
                ) {
                    `in`?.also {
                        val bytes = ByteArray(it.readableBytes())
                        it.readBytes(bytes)
                        out?.add(String(bytes))
                    }
                }

            })
            addLast(object : SimpleChannelInboundHandler<String>() {
                override fun channelRead0(
                    ctx: ChannelHandlerContext?,
                    msg: String?
                ) {
                    receiveDataLogs.value += "${ctx?.channel()?.remoteAddress()}：\n$msg"
//                                    appendLog("${ctx?.channel()?.remoteAddress()}：\n$msg")
                }

                override fun channelActive(ctx: ChannelHandlerContext?) {
                    super.channelActive(ctx)
                    receiveDataLogs.value += "${ctx?.channel()?.remoteAddress()}：与服务端连接成功"
                    serviceStatus.value = ServiceStatus.running
                }

                override fun channelInactive(ctx: ChannelHandlerContext?) {
                    super.channelInactive(ctx)
                    serviceStatus.value = ServiceStatus.idle
                }
            })
        }
    }

    fun start() {
        if (serviceStatus.value != ServiceStatus.idle) return
        serviceStatus.value = ServiceStatus.starting
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (socketProtocol.value) {
            SocketProtocol.tcpServer -> {
                bootstrap.channel(NioServerSocketChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioServerSocketChannel>() {
                    override fun initChannel(ch: NioServerSocketChannel?) {
                        initNettyChannel(ch)
                    }
                })
            }
            SocketProtocol.tcpClient -> {
                bootstrap.channel(NioSocketChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioSocketChannel>() {
                    override fun initChannel(ch: NioSocketChannel?) {
                        initNettyChannel(ch)
                    }
                })
            }
            SocketProtocol.udp -> {
                bootstrap.channel(NioDatagramChannel::class.java)
                bootstrap.handler(object : ChannelInitializer<NioDatagramChannel>() {
                    override fun initChannel(ch: NioDatagramChannel?) {
                        initNettyChannel(ch)
                    }
                })
            }
        }
        bootstrap.connect(ip.value, port.value)
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
            }
        }
    }
}