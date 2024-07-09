package com.hbsoo.room.action;

import com.hbsoo.access.control.AccessLimit;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
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
@AccessLimit(globalRateSize = 1000)
@OuterServerMessageHandler(1001)
public class ReceiveChatRoomMessageAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ReceiveChatRoomMessageAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String message = decoder.readStr();
        long userId = decoder.readLong();
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom");
        //测试发送同步消息
        //HBSPackage.Decoder result = request2Server(decoder.toBuilder(), 10, (builder) -> forward2InnerServer(builder, "gateway", userId));
        //System.out.println("result = " + result);
        if (Objects.isNull(chatRoom)) {
            forward2AllInnerServerByTypeUseSender(decoder.toBuilder(), "room");
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
