package com.hbsoo.server.client;

import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.function.Consumer;

public final class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    private final Bootstrap bootstrap;
    private final Consumer<Bootstrap> connectFun;
    public HeartbeatHandler(Bootstrap bootstrap, Consumer<Bootstrap> connectFun) {
        this.bootstrap = bootstrap;
        this.connectFun = connectFun;
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                // 发送心跳包
                byte[] msg = HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.InnerMessageType.HEARTBEAT).buildPackage();
                ByteBuf buf = Unpooled.wrappedBuffer(msg);
                ctx.writeAndFlush(buf).sync();
                //System.out.println("Sent heartbeat to server");
                final SocketAddress socketAddress = ctx.channel().remoteAddress();
                final String remoteAddr = socketAddress.toString();
                logger.trace("Sent heartbeat to server:{}", remoteAddr);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("Channel inactive, closing connection");
        final SocketAddress socketAddress = ctx.channel().remoteAddress();
        final String remoteAddr = socketAddress.toString();
        logger.warn("Channel inactive, closing connection,remoteAddress:{}", remoteAddr);
        ctx.close();
        // 重连
        connectFun.accept(this.bootstrap);
    }
}
