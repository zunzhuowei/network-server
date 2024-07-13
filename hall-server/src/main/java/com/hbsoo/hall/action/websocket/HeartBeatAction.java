package com.hbsoo.hall.action.websocket;

import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@OutsideMessageHandler(99)
public class HeartBeatAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        UserSession userSession = extendBody.getUserSession();
        Long userId = userSession.getId();
        logger.debug("收到心跳消息:{}", userId);
        outsideUserSessionManager.sendMsg2User(
                OutsideUserProtocol.BINARY_WEBSOCKET,
                decoder.toBuilder(),
                userId
        );
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
