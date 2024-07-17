package com.hbsoo.room.action.inside;

import com.google.gson.Gson;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.room.entity.Card;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(101)
public class JoinGameRoomUseThreadAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(JoinGameRoomUseThreadAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        OutsideUserProtocol protocol = extendBody.getOutsideUserProtocol();
        String username = decoder.readStr();
        String roomName = decoder.readStr();
        long userId = decoder.readInt();
        long roomId = decoder.readLong();
        String channelId = extendBody.getUserChannelId();
        logger.info("JoinGameRoomUseThreadAction username:{}，channelId:{}，userId:{}", username, channelId, userId);
        GameRoom gameRoom = GameRoomManager.getGameRoom(roomName);
        if (Objects.isNull(gameRoom)) {
            NetworkPacket.Builder builder = NetworkPacket.Builder
                    .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                    .msgType(101).writeStr("房间不存在");
            outsideUserSessionManager.sendMsg2User(protocol, builder, userId);
            return;
        }
        Seat[] seats = gameRoom.getSeats();

        //加入房间
        int seatNo = 0;
        boolean isJoin = false;
        for (Seat seat : seats) {
            if (Objects.nonNull(seat)) {
                Long id = seat.userSession.getId();
                if (userId == id) {
                    isJoin = true;
                    break;
                }
                seatNo++;
                continue;
            }
            UserSession userSession = extendBody.getUserSession();
            if (Objects.isNull(userSession)) {
                continue;
            }
            seat = new Seat();
            seat.seatNo = seatNo;
            seat.cardsInHand = new ArrayList<>();
            seat.cardsOutHand = new ArrayList<>();
            seat.userSession = userSession;
            seat.isGrab = false;
            isJoin = true;
            seats[seatNo] = seat;
            break;
        }
        if (!isJoin) {
            NetworkPacket.Builder builder = NetworkPacket.Builder
                    .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                    .msgType(101).writeStr("房间已满");
            outsideUserSessionManager.sendMsg2User(protocol, builder, userId);
            return;
        }

        //通知用户成功加入房间
        Gson gson = new Gson();
        NetworkPacket.Builder builder = NetworkPacket.Builder
                .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                .msgType(101)
                .writeStr("房间name,id:" + gameRoom.getRoomName() + ":" + gameRoom.getRoomId());
        outsideUserSessionManager.sendMsg2User(protocol, builder, userId);

        //通知他人该用户加入了房间
        NetworkPacket.Builder onlineMsg = NetworkPacket.Builder.withDefaultHeader()
                .msgType(100).writeStr(username).writeLong(userId);
        for (Seat seat : seats) {
            if (Objects.isNull(seat)) {
                continue;
            }
            Long id = seat.userSession.getId();
            if (!id.equals(userId)) {
                outsideUserSessionManager.sendMsg2User(
                        seat.userSession.getOutsideUserProtocol(),
                        onlineMsg,
                        seat.userSession.getId()
                );
            }
        }
        //游戏已经开始
        int status = gameRoom.getStatus();
        if (status == 1) {
            //断线重连重新加入
            Optional<Seat> first = Arrays.stream(seats).filter(Objects::nonNull)
                    .filter(seat -> Objects.nonNull(seat.userSession))
                    .filter(seat -> seat.userSession.getId().equals(userId))
                    .findFirst();
            first.ifPresent(seat -> {
                NetworkPacket.Builder playerCardBuilder = NetworkPacket.Builder
                        .withDefaultHeader()
                        .msgType(103)
                        .writeStr(gson.toJson(seat.cardsInHand));
                outsideUserSessionManager.sendMsg2User(
                        seat.userSession.getOutsideUserProtocol(),
                        playerCardBuilder,
                        seat.userSession.getId()
                );
            });
            return;
        }

        startGame(ctx, gameRoom, seats, gson, status);
    }

    public void startGame(ChannelHandlerContext ctx, GameRoom gameRoom, Seat[] seats, Gson gson, int status) {
        //如果房间已经满人，开始发牌
        boolean isFullSeat = Arrays.stream(seats).allMatch(Objects::nonNull);
        if (isFullSeat && status == 0) {
            List<Card> cards = Card.newCards();
            int offset = 0;
            //地主牌
            gameRoom.setDiZhuCards(cards.subList(offset, 3).toArray(new Card[0]));
            offset += 3;
            //发牌每人17张
            for (Seat seat : seats) {
                seat.cardsInHand = new ArrayList<>(cards.subList(offset, offset + 17));
                seat.cardsOutHand = new ArrayList<>();
                //seat.cardsInHand = cards.subList(offset, offset + 17);
                //java.util.ConcurrentModificationException
                offset += 17;
            }
            //通知玩家发牌
            for (Seat seat : seats) {
                if (Objects.isNull(seat)) {
                    continue;
                }
                //掉线了
                if (seat.userSession == null) {
                    continue;
                }
                NetworkPacket.Builder playerCardBuilder = NetworkPacket.Builder
                        .withDefaultHeader()
                        .msgType(103)
                        .writeStr(gson.toJson(seat.cardsInHand));
                outsideUserSessionManager.sendMsg2User(
                        seat.userSession.getOutsideUserProtocol(),
                        playerCardBuilder,
                        seat.userSession.getId()
                );
            }
            //游戏开始
            gameRoom.setStatus(1);
            //自动出牌
            NetworkPacket.Builder autoDiscardBuilder = NetworkPacket.Builder
                    .withDefaultHeader()
                    .msgType(1001)
                    .writeBoolean(true)
                    .writeStr(gameRoom.getRoomName())
                    ;
            redirectMessage(ctx, autoDiscardBuilder,1);
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        String username = decoder.readStr();
        String roomName = decoder.readStr();
        long userId = decoder.readInt();
        long roomId = decoder.readLong();
        return roomId;
    }
}
