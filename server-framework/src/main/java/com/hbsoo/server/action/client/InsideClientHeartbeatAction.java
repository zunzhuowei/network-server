package com.hbsoo.server.action.client;

import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收服务端返回的心跳消息
 * Created by zun.wei on 2024/6/6.
 */
@InsideClientMessageHandler(MessageType.Inside.HEARTBEAT)
public class InsideClientHeartbeatAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideClientHeartbeatAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.trace("InsideClientHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return ctx.channel().id().asShortText();
    }

}
