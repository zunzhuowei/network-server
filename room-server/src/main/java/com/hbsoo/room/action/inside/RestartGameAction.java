package com.hbsoo.room.action.inside;

import com.google.gson.Gson;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 重新开始游戏
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(1003)
public class RestartGameAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(RestartGameAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;
    @Autowired
    private JoinGameRoomUseThreadAction joinGameRoomUseThreadAction;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        String roomName = decoder.readStr();
        GameRoom gameRoom = GameRoomManager.getGameRoom(roomName);
        if (Objects.isNull(gameRoom) || gameRoom.getStatus() != 2) {
            logger.debug("房间状态异常");
            return;
        }
        joinGameRoomUseThreadAction.startGame(ctx, gameRoom, gameRoom.getSeats(), new Gson(), 0);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }


}
