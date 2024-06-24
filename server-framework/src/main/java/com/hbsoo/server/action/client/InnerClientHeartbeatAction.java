package com.hbsoo.server.action.client;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收服务端返回的心跳消息
 * Created by zun.wei on 2024/6/6.
 */
@InnerClientMessageHandler(HBSMessageType.Inner.HEARTBEAT)
public class InnerClientHeartbeatAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientHeartbeatAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.trace("InnerClientHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return ctx.channel().id().asShortText();
    }

}
