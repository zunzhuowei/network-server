package com.hbsoo.room.action;

import com.google.gson.Gson;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.room.action.inside.JoinGameRoomUseThreadAction;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 用户准备游戏
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@OutsideMessageHandler(1006)
public class ReadyStartGameAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ReadyStartGameAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;
    @Autowired
    private JoinGameRoomUseThreadAction joinGameRoomUseThreadAction;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        if (Objects.isNull(gameRooms) || gameRooms.isEmpty()) {
            forward2AllInsideServerByTypeUseSender(decoder.toBuilder().msgType(1006), "room");
            return;
        }
        boolean match = gameRooms.stream().anyMatch(gameRoom -> gameRoom.getStatus() == 0);
        if (!match) {
            return;
        }
        gameRooms.forEach(gameRoom -> {
            if (gameRoom.getStatus() == 0) {
                Seat[] seats = gameRoom.getSeats();
                for (int i = 0; i < seats.length; i++) {
                    Seat seat = seats[i];
                    if (seat.userSession.getId().equals(userId)) {
                        seat.isReady = true;
                        break;
                    }
                }
            }
        });
        //通知客户端已准备
        NetworkPacket.Builder ready = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(1007);
        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, ready, userId);
        gameRooms.forEach(gameRoom -> {
            if (gameRoom.getStatus() == 0) {
                Seat[] seats = gameRoom.getSeats();
                boolean allMatch = Arrays.stream(seats).allMatch(seat -> seat.isReady);
                //全部准备好则开始游戏
                if (allMatch) {
                    //完成准备
                    gameRoom.setStatus(1);
                    //通知抢地主
                    joinGameRoomUseThreadAction.startGame(ctx, gameRoom, seats, new Gson(), gameRoom.getStatus());
                    //joinGameRoomUseThreadAction.notifyUserGrab(seats);
                }
            }
        });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        if (Objects.nonNull(gameRooms) && !gameRooms.isEmpty()) {
            return gameRooms.get(0).getRoomId();
        }
        return userId;
    }

}
