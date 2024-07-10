package com.hbsoo.room.action.inside;

import com.google.gson.Gson;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.session.OutsideUserProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(100)
public class JoinChatRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(JoinChatRoomAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession1 = decoder.readUserSession();
        String username = decoder.readStr();
        final String channelId = userSession1.getChannelId();
        long userId = decoder.readInt();
        //1.执行登录流程，同步到到网关服。
        //2.判断聊天房间是否存在
        //3.存在则进入，不存在则创建；进入聊天房间
        //4.发送聊天小则走房间服务。
        logger.info("JoinChatRoomAction username:{}，channelId:{}，userId:{}", username, channelId, userId);
        UserSession userSession = outsideUserSessionManager.getUserSession(userId);
        //有可能网关还未同步用户信息过来
        if (userSession == null) {
            delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder.toBuilder()), 500L, TimeUnit.MILLISECONDS);
            return;
        }
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom",k -> {
            ChatRoom chatRoom1 = new ChatRoom();
            chatRoom1.setRoomName("first-chatroom");
            chatRoom1.setRoomId(1L);
            chatRoom1.setBelongServerId(outsideUserSessionManager.getNowServerInfo().getId());
            chatRoom1.setBelongServerType(outsideUserSessionManager.getNowServerInfo().getType());
            CopyOnWriteArraySet<Long> userIds = new CopyOnWriteArraySet<>();
            chatRoom1.setUserIds(userIds);
            chatRoom1.setRecentMsgList(new CopyOnWriteArrayList<>());
            return chatRoom1;
        });
        //通知他人该用户上线了
        NetworkPacket.Builder onlineMsg = NetworkPacket.Builder.withDefaultHeader()
                .msgType(100).writeStr(username).writeLong(userId);
        outsideUserSessionManager.sendMsg2User(
                OutsideUserProtocol.BINARY_WEBSOCKET,
                onlineMsg,
                chatRoom.getUserIds().stream().filter(id -> !id.equals(userId)).distinct().toArray(Long[]::new)
        );

        chatRoom.addUser(userId);
        Gson gson = new Gson();
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(101)
                .writeStr(gson.toJson(chatRoom));
        //通知用户成功加入房间
        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, userId);
        //推送历史消息
        List<String> recentMsgList = chatRoom.getRecentMsgList();
        for (String msg : recentMsgList) {
             String[] split = msg.split(":");
            NetworkPacket.Builder historyMsg = NetworkPacket.Builder.withDefaultHeader()
                    .msgType(1001).writeLong(Long.parseLong(split[0])).writeStr(split[1]);
            outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, historyMsg, userId);
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
