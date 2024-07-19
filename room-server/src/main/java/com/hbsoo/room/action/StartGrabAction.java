package com.hbsoo.room.action;

import com.alibaba.fastjson.JSON;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.room.action.inside.JoinGameRoomUseThreadAction;
import com.hbsoo.room.entity.Card;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户开始抢地主
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@OutsideMessageHandler(1008)
public class StartGrabAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(StartGrabAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        if (Objects.isNull(gameRooms) || gameRooms.isEmpty()) {
            forward2AllInsideServerByTypeUseSender(decoder.toBuilder().msgType(1008), "room");
            return;
        }
        boolean match = gameRooms.stream().anyMatch(gameRoom -> gameRoom.getStatus() == 1);
        if (!match) {
            return;
        }
        gameRooms.forEach(gameRoom -> {
            if (gameRoom.getStatus() == 1) {
                Seat[] seats = gameRoom.getSeats();
                for (int i = 0; i < seats.length; i++) {
                    Seat seat = seats[i];
                    if (seat.userSession.getId().equals(userId)) {
                        seat.isGrab = true;
                        break;
                    }
                }
            }
        });
        //通知客户端已抢地主
        NetworkPacket.Builder ready = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(1009);
        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, ready, userId);
        gameRooms.forEach(gameRoom -> {
            if (gameRoom.getStatus() == 1) {
                Seat[] seats = gameRoom.getSeats();
                boolean allMatch = Arrays.stream(seats).allMatch(seat -> seat.isGrab);
                //全部准备好则开始游戏
                if (allMatch) {
                    //完成抢地主
                    Random random = new Random();
                    int i = random.nextInt();
                    int index = Math.abs(Objects.hash(i)) % 3;
                    gameRoom.setStatus(2);
                    gameRoom.setTurnNo(index);
                    gameRoom.getSeats()[0].isLandlord = index == 0;
                    gameRoom.getSeats()[1].isLandlord = index == 1;
                    gameRoom.getSeats()[2].isLandlord = index == 2;
                    for (Card diZhuCard : gameRoom.getDiZhuCards()) {
                        gameRoom.getSeats()[index].cardsInHand.add(diZhuCard);
                    }
                   //通知客户端地主牌和地主是谁
                    NetworkPacket.Builder diZhuCardsBuilder = NetworkPacket.Builder
                            .withDefaultHeader()
                            .msgType(1010)
                            .writeInt(index)
                            .writeStr(JSON.toJSONString(gameRoom.getDiZhuCards()));
                    for (Seat seat : seats) {
                        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, diZhuCardsBuilder, seat.userSession.getId());
                    }
                    //自动出牌
                    NetworkPacket.Builder autoDiscardBuilder = NetworkPacket.Builder
                            .withDefaultHeader()
                            .msgType(1001)
                            .writeBoolean(true)
                            .writeStr(gameRoom.getRoomName());
                    redirectAndSwitchProtocol(ctx, ProtocolType.INSIDE_TCP, autoDiscardBuilder, 1, TimeUnit.SECONDS);
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
