package com.hbsoo.server.action;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerTcpClientMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.session.ServerType;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收服务端返回的心跳消息
 * Created by zun.wei on 2024/6/6.
 */
@InnerClientMessageHandler(HBSMessageType.InnerMessageType.HEARTBEAT)
public class InnerClientHeartbeatAction extends InnerTcpClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientHeartbeatAction.class);

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String remoteAddr = ctx.channel().remoteAddress().toString();
        logger.debug("InnerClientHeartbeatAction 心跳包：remoteAddr {}", remoteAddr);
    }

}
