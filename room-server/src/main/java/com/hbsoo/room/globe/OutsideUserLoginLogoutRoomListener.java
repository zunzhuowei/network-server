package com.hbsoo.room.globe;

import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.OutsideUserLoginLogoutListener;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by zun.wei on 2024/7/15.
 */
@Component
public class OutsideUserLoginLogoutRoomListener implements OutsideUserLoginLogoutListener {

    private static final Logger logger = LoggerFactory.getLogger(OutsideUserLoginLogoutRoomListener.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void onLogin(Long userId, UserSession userSession) {
        logger.debug("onLogin userId:{}", userId);
    }

    @Override
    public void onLogout(Long userId) {
        logger.debug("onLogout userId:{}", userId);
        List<GameRoom> gameRooms = GameRoomManager.findGameRoomByUserId(userId);
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(102).writeLong(userId).writeStr("offline");
//        for (GameRoom gameRoom : gameRooms) {
//            gameRoom.getUserSessions().forEach(us -> {
//                if (us.getOutsideUserProtocol() == OutsideUserProtocol.UDP) {
//                    builder.replaceHeader(NetworkPacket.UDP_HEADER);
//                }
//                outsideUserSessionManager.sendMsg2User(
//                        us.getOutsideUserProtocol(),
//                        builder,
//                        us.getId()
//                );
//                if (us.getOutsideUserProtocol() == OutsideUserProtocol.UDP) {
//                    builder.replaceHeader(NetworkPacket.TCP_HEADER);
//                }
//            });
//        }
        //GameRoomManager.quitGameRoom(userId);

    }

}
