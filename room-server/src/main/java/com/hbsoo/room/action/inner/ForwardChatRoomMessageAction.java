package com.hbsoo.room.action.inner;

import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSessionProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InnerServerMessageHandler(1001)
public class ForwardChatRoomMessageAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ForwardChatRoomMessageAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String message = decoder.readStr();
        long userId = decoder.readLong();
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom");
        if (Objects.isNull(chatRoom)) {
            logger.error("chatRoom is null");
            return;
        }
        chatRoom.addRecentMsg(userId + ":" + message);
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(1001).writeLong(userId).writeStr(message);
        outerUserSessionManager.sendMsg2User(
                UserSessionProtocol.binary_websocket,
                builder,
                chatRoom.getUserIds().toArray(new Long[0])
        );
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom");
        if (Objects.isNull(chatRoom)) {
            return null;
        }
        return chatRoom.getRoomId();
    }
}
