package com.hbsoo.room.action.inner;

import com.google.gson.Gson;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.session.UserSessionProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InnerServerMessageHandler(100)
public class JoinChatRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(JoinChatRoomAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String username = decoder.readStr();
        String channelId = decoder.readStr();
        long userId = decoder.readInt();
        //1.执行登录流程，同步到到网关服。
        //2.判断聊天房间是否存在
        //3.存在则进入，不存在则创建；进入聊天房间
        //4.发送聊天小则走房间服务。
        logger.info("JoinChatRoomAction username:{}，channelId:{}，userId:{}", username, channelId, userId);
        UserSession userSession = outerUserSessionManager.getUserSession(userId);
        //有可能网关还未同步用户信息过来
        if (userSession == null) {
            delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder.toBuilder()), 500L, TimeUnit.MILLISECONDS);
            return;
        }
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom",k -> {
            ChatRoom chatRoom1 = new ChatRoom();
            chatRoom1.setRoomName("first-chatroom");
            chatRoom1.setRoomId(1L);
            chatRoom1.setBelongServerId(outerUserSessionManager.getNowServerInfo().getId());
            chatRoom1.setBelongServerType(outerUserSessionManager.getNowServerInfo().getType());
            CopyOnWriteArraySet<Long> userIds = new CopyOnWriteArraySet<>();
            chatRoom1.setUserIds(userIds);
            chatRoom1.setRecentMsgList(new CopyOnWriteArrayList<>());
            return chatRoom1;
        });
        //通知上线
        Set<Long> userIds = chatRoom.getUserIds();
        outerUserSessionManager.sendMsg2User(UserSessionProtocol.binary_websocket,
                decoder.toBuilder(), userIds.toArray(new Long[0]));

        chatRoom.addUser(userId);
        Gson gson = new Gson();
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(101)
                .writeStr(gson.toJson(chatRoom));
        //通知用户
        outerUserSessionManager.sendMsg2User(UserSessionProtocol.binary_websocket, builder, userId);
        //推送历史消息
        List<String> recentMsgList = chatRoom.getRecentMsgList();
        for (String msg : recentMsgList) {
             String[] split = msg.split(":");
            HBSPackage.Builder historyMsg = HBSPackage.Builder.withDefaultHeader()
                    .msgType(1001).writeLong(Long.parseLong(split[0])).writeStr(split[1]);
            outerUserSessionManager.sendMsg2User(UserSessionProtocol.binary_websocket, historyMsg, userId);
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readStr();
    }
}
