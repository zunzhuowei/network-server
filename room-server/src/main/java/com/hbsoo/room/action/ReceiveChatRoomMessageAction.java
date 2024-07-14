package com.hbsoo.room.action;

import com.hbsoo.access.control.AccessLimit;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@AccessLimit(globalRateSize = 1000)
@OutsideMessageHandler(1001)
public class ReceiveChatRoomMessageAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveChatRoomMessageAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        String message = decoder.readStr();
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom");
        //测试发送同步消息
        NetworkPacket.Decoder result = request2Server(decoder.toBuilder(), 10, (builder) -> forward2InsideServer(builder, "gateway", userId));
        System.out.println("result = " + result.readStr());
        if (Objects.isNull(chatRoom)) {
            forward2AllInsideServerByTypeUseSender(decoder.toBuilder(), "room");
            return;
        }
        chatRoom.addRecentMsg(userId + ":" + message);
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(1001).writeLong(userId).writeStr(message);
        chatRoom.getUserSessions().forEach(us -> {
            if (us.getOutsideUserProtocol() == OutsideUserProtocol.UDP) {
                builder.replaceHeader(NetworkPacket.UDP_HEADER);
            }
            outsideUserSessionManager.sendMsg2User(
                    us.getOutsideUserProtocol(),
                    builder,
                    us.getId()
            );
            if (us.getOutsideUserProtocol() == OutsideUserProtocol.UDP) {
                builder.replaceHeader(NetworkPacket.TCP_HEADER);
            }
        });

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom");
        if (Objects.isNull(chatRoom)) {
            return null;
        }
        return chatRoom.getRoomId();
    }
}
