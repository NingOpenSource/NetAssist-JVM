//package org.ning1994.net_assist.core
//
//import io.netty.bootstrap.AbstractBootstrap
//import io.netty.channel.Channel
//import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelInboundHandlerAdapter
//import io.netty.channel.SimpleChannelInboundHandler
//import io.netty.channel.socket.SocketChannel
//import io.netty.handler.codec.bytes.ByteArrayEncoder
//import io.netty.handler.codec.string.StringDecoder
//import io.netty.handler.codec.string.StringEncoder
//
//abstract class SocketHelper(
//    private val receiver: (channel: Channel, data: ByteArray) -> Unit
//) {
//
//    private fun initNettyChannel(ch: Channel?) {
//        rootChannel = ch
//        ch?.pipeline()?.apply {
//            addLast(StringEncoder())
//            addLast(ByteArrayEncoder())
////            addLast(ByteArrayDecoder())
//            addLast(StringDecoder())
//            addLast(object : SimpleChannelInboundHandler<String>() {
//                override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
//                    super.channelRead(ctx, msg)
//                    if (msg != null && msg is SocketChannel && !remoteClientInfoList.contains(msg)) {
//                        remoteClientInfoList.add(msg)
//                        remoteLog(msg, "已接受远程主机连接到本地")
//                    }
//                }
//
//                override fun channelRead0(
//                    ctx: ChannelHandlerContext?,
//                    msg: String?
//                ) {
//                    remoteLog(ctx!!.channel(), msg!!)
//                }
//
//                override fun channelActive(ctx: ChannelHandlerContext?) {
//                    super.channelActive(ctx)
//                    if (socketProtocol.value == SocketProtocol.tcpServer) {
//                        if (ctx?.channel() != null) {
//                            log("已连接到服务器:${ctx.channel()}：TCP服务器启动完成")
//                        } else {
//                            localLog("TCP服务器启动完成")
//                        }
//                    } else {
//                        log("已连接到服务器!")
//                    }
//                    if (serviceStatus.value == ServiceStatus.starting) {
//                        serviceStatus.value = ServiceStatus.running
//                    }
//                }
//
//                override fun channelInactive(ctx: ChannelHandlerContext?) {
//                    super.channelInactive(ctx)
//                    remoteLog(ctx!!.channel(), "服务端断开连接...")
//                    if (serviceStatus.value == ServiceStatus.stopping
//                        || serviceStatus.value == ServiceStatus.running
//                    ) {
//                        serviceStatus.value = ServiceStatus.idle
//                    }
//                }
//            })
//                .addLast(object : ChannelInboundHandlerAdapter() {
//                    override fun channelActive(ctx: ChannelHandlerContext?) {
//                        super.channelActive(ctx)
//                    }
//
//                    override fun channelInactive(ctx: ChannelHandlerContext?) {
//                        super.channelInactive(ctx)
//                    }
//                })
//        }
//    }
//
//    abstract fun start()
//
//    abstract fun stop()
//
//    abstract fun send(data: ByteArray)
//}
//
//class TCPSocketHelper(receiver: (channel: Channel, data: ByteArray) -> Unit) : SocketHelper(receiver) {
//    override fun start() {
//
//    }
//
//    override fun stop() {
//
//    }
//
//    override fun send(data: ByteArray) {
//
//    }
//}