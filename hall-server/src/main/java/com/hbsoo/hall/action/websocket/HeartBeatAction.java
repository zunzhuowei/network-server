package com.hbsoo.hall.action.websocket;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSessionProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(99)
public class HeartBeatAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        Long userId = decoder.readLong();
        logger.debug("收到心跳消息:{}", userId);
        outerUserSessionManager.sendMsg2User(
                UserSessionProtocol.binary_websocket,
                decoder.toBuilder(),
                userId
        );
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
