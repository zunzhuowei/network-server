package com.hbsoo.room.action.inside;

import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.room.entity.Card;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.session.OutsideUserProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(100)
public class JoinGameRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(JoinGameRoomAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        OutsideUserProtocol protocol = extendBody.getOutsideUserProtocol();
        String username = decoder.readStr();
        String roomName = decoder.readStr();
        long userId = decoder.readInt();
        final String channelId = extendBody.getUserChannelId();
        logger.info("JoinGameRoomAction username:{}，channelId:{}，userId:{}", username, channelId, userId);
        UserSession userSession = outsideUserSessionManager.getUserSession(userId);
        //有可能网关还未同步用户信息过来
        if (userSession == null) {
            int retryTimes = extendBody.getRetryTimes();
            if (retryTimes < 5) {
                extendBody.setRetryTimes(retryTimes + 1);
                delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder.replaceExtendBody(extendBody)), 500, TimeUnit.MILLISECONDS);
            } else {
                NetworkPacket.Builder builder = NetworkPacket.Builder
                        .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                        .msgType(101)
                        .writeStr("join game room failed");
                outsideUserSessionManager.sendMsg2User(protocol, builder, userId);
            }
            return;
        }
        extendBody.setUserSession(userSession);
        extendBody.setRetryTimes(0);
        decoder.replaceExtendBody(extendBody);
        GameRoom gameRoom = GameRoomManager.getGameRoom(roomName, k -> {
            GameRoom gr = new GameRoom();
            gr.setRoomName(k);
            gr.setRoomId((long) Objects.hash(roomName));
            gr.setBelongServerId(outsideUserSessionManager.getNowServerInfo().getId());
            gr.setBelongServerType(outsideUserSessionManager.getNowServerInfo().getType());
            Seat[] seats = new Seat[3];
            Card[] diZhuCards = new Card[3];
            gr.setSeats(seats);
            gr.setDiZhuCards(diZhuCards);
            gr.setTimer(10);
            return gr;
        });
        //join to specified thread and join game room
        redirectMessage(ctx, decoder.toBuilder().msgType(101).writeLong(gameRoom.getRoomId()));
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
