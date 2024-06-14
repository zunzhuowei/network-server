package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/10.
 */
@InnerServerMessageHandler(HBSMessageType.InnerMessageType.HEARTBEAT)
public class InnerServerHeartbeatAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerHeartbeatAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.trace("InnerHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
        // 发送心跳包
        byte[] msg = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.InnerMessageType.HEARTBEAT).buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(msg);
        ctx.writeAndFlush(buf);

        /*redirectAndSwitchProtocol(ctx,
                ProtocolType.INNER_WEBSOCKET,
                HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.InnerMessageType.LOGOUT)
        );*/

        /*HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.InnerMessageType.HEARTBEAT)
                .writeInt(1);
        redirectMessage(ctx, builder);*/
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
