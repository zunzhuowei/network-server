package com.hbsoo.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Objects;

/**
 * 长时间没有收到客户端的消息，则断开链接；
 */
public final class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerHeartbeatHandler.class);


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                // 发送心跳包
                //byte[] msg = HBSPackage.Builder.withDefaultHeader()
                //        .msgType(HBSMessageType.InnerMessageType.HEARTBEAT).buildPackage();
                //ByteBuf buf = Unpooled.wrappedBuffer(msg);
                //ctx.writeAndFlush(buf).sync();
                SocketAddress socketAddress = ctx.channel().remoteAddress();
                String remoteAddr = socketAddress.toString();

                Integer idleTimes = ctx.channel().attr(AttributeKeyConstants.idleTimesKey).get();
                if (idleTimes != null && idleTimes >= 3) {
                    logger.warn("Channel inactive, closing connection,remoteAddress:{}", remoteAddr);
                    ctx.close();
                    return;
                }
                ctx.channel().attr(AttributeKeyConstants.idleTimesKey).set(idleTimes == null ? 1 : idleTimes + 1);
                logger.trace("not received heartbeat from client:{},times:{}", remoteAddr, idleTimes);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 只有登录的用户，才可以重置心跳空闲次数
        Long id = ctx.channel().attr(AttributeKeyConstants.idAttr).get();
        if (Objects.nonNull(id)) {
            ctx.channel().attr(AttributeKeyConstants.idleTimesKey).set(null);
        }
        //流转到下一个handler
        super.channelRead(ctx, msg);
    }

//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        final SocketAddress socketAddress = ctx.channel().remoteAddress();
//        final String remoteAddr = socketAddress.toString();
//        logger.warn("Channel inactive, closing connection,remoteAddress:{}", remoteAddr);
//        ctx.close();
//    }
}
