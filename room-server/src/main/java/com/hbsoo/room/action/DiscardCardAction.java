package com.hbsoo.room.action;

import com.alibaba.fastjson.JSON;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.room.entity.Card;
import com.hbsoo.room.entity.CardType;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户出牌消息
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@OutsideMessageHandler(1001)
public class DiscardCardAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DiscardCardAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        String cardsJson = decoder.readStr();
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        if (Objects.isNull(gameRooms) || gameRooms.isEmpty()) {
            forward2AllInsideServerByTypeUseSender(decoder.toBuilder(), "room");
            return;
        }
        //转发到自动出牌处理器处理
        NetworkPacket.Builder autoDiscardBuilder = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(1001)
                .writeBoolean(false)
                .writeStr(gameRooms.get(0).getRoomName())
                .writeLong(userId)
                .writeStr(cardsJson);
        redirectAndSwitchProtocol(ctx, ProtocolType.INSIDE_TCP, autoDiscardBuilder);
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
