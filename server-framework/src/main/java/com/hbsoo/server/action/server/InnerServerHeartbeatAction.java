package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务接收内部客户端心跳包
 * Created by zun.wei on 2024/6/10.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.HEARTBEAT)
public class InnerServerHeartbeatAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerHeartbeatAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.trace("InnerServerHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
        // 发送心跳包
        HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Inner.HEARTBEAT)
                .buildAndSendBytesTo(ctx.channel());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
