package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务接收内部客户端心跳包
 * Created by zun.wei on 2024/6/10.
 */
@InsideServerMessageHandler(MessageType.Inside.HEARTBEAT)
public class InsideServerHeartbeatAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerHeartbeatAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.trace("InsideServerHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
        // 发送心跳包
        NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.HEARTBEAT)
                .sendTcpTo(ctx.channel());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
