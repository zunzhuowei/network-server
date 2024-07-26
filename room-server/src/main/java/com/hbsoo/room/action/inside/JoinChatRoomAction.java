package com.hbsoo.room.action.inside;

import com.google.gson.Gson;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.ChatRoom;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

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
        ExtendBody extendBody = decoder.readExtendBody();
        OutsideUserProtocol protocol = extendBody.getOutsideUserProtocol();
        String username = decoder.readStr();
        final String channelId = extendBody.getUserChannelId();
        long userId = decoder.readInt();
        logger.info("JoinChatRoomAction username:{}，channelId:{}，userId:{}", username, channelId, userId);
        UserSession userSession = outsideUserSessionManager.getUserSession(userId);
        //有可能网关还未同步用户信息过来
        if (userSession == null) {
            int retryTimes = extendBody.getRetryTimes();
            if (retryTimes < 5) {
                extendBody.setRetryTimes(retryTimes + 1);
                delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder.replaceExtendBody(extendBody)), 500L, TimeUnit.MILLISECONDS);
            }
            return;
        }
        ChatRoom chatRoom = ChatRoomManager.getChatRoom("first-chatroom",k -> {
            ChatRoom chatRoom1 = new ChatRoom();
            chatRoom1.setRoomName("first-chatroom");
            chatRoom1.setRoomId(1L);
            chatRoom1.setBelongServerId(outsideUserSessionManager.getNowServerInfo().getId());
            chatRoom1.setBelongServerType(outsideUserSessionManager.getNowServerInfo().getType());
            CopyOnWriteArraySet<UserSession> userSessions = new CopyOnWriteArraySet<>();
            chatRoom1.setUserSessions(userSessions);
            chatRoom1.setRecentMsgList(new CopyOnWriteArrayList<>());
            return chatRoom1;
        });
        //通知他人该用户上线了
        NetworkPacket.Builder onlineMsg = NetworkPacket.Builder.withDefaultHeader()
                .msgType(100).writeStr(username).writeLong(userId);
        chatRoom.getUserSessions().forEach(us -> {
            if (!us.getId().equals(userId)) {
                outsideUserSessionManager.sendMsg2User(
                        us.getOutsideUserProtocol(),
                        onlineMsg,
                        us.getId()
                );
            }
        });
        //加入房间
        chatRoom.addUser(userSession);
        Gson gson = new Gson();
        NetworkPacket.Builder builder = NetworkPacket.Builder
                .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                .msgType(101)
                .writeStr(gson.toJson(chatRoom));
        //通知用户成功加入房间
        outsideUserSessionManager.sendMsg2User(protocol, builder, userId);
        //推送历史消息
        List<String> recentMsgList = chatRoom.getRecentMsgList();
        for (String msg : recentMsgList) {
             String[] split = msg.split(":");
            NetworkPacket.Builder historyMsg = NetworkPacket.Builder
                    .withHeader(protocol == OutsideUserProtocol.UDP ? NetworkPacket.UDP_HEADER : NetworkPacket.TCP_HEADER)
                    .msgType(1001).writeLong(Long.parseLong(split[0])).writeStr(split[1]);
            outsideUserSessionManager.sendMsg2User(protocol, historyMsg, userId);
        }
        //just test
        redirectAndSwitchProtocol(ctx, decoder, TestRedirectByHandlerAction.class);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
