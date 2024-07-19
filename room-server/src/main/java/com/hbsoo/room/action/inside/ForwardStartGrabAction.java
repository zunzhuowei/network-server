package com.hbsoo.room.action.inside;

import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(1008)
public class ForwardStartGrabAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ForwardStartGrabAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        if (Objects.isNull(gameRooms)) {
            logger.error("chatRoom is null");
            return;
        }
        redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_WEBSOCKET, decoder.toBuilder().msgType(1008));
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
