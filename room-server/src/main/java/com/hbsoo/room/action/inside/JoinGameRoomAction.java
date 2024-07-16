package com.hbsoo.room.action.inside;

import com.hbsoo.room.ChatRoomManager;
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
            }
            return;
        }
        extendBody.setUserSession(userSession);
        extendBody.setRetryTimes(0);
        decoder.replaceExtendBody(extendBody);
        GameRoom gameRoom = ChatRoomManager.getGameRoom(roomName, k -> {
            GameRoom gameRoom1 = new GameRoom();
            gameRoom1.setRoomName(k);
            gameRoom1.setRoomId((long) Objects.hash(roomName));
            gameRoom1.setBelongServerId(outsideUserSessionManager.getNowServerInfo().getId());
            gameRoom1.setBelongServerType(outsideUserSessionManager.getNowServerInfo().getType());
            Seat[] seats = new Seat[3];
            Card[] diZhuCards = new Card[3];
            gameRoom1.setSeats(seats);
            gameRoom1.setDiZhuCards(diZhuCards);
            return gameRoom1;
        });
        redirectMessage(ctx, decoder.toBuilder().msgType(101).writeLong(gameRoom.getRoomId()));
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
