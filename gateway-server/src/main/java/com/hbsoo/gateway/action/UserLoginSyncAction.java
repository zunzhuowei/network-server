package com.hbsoo.gateway.action;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.netty.ProtocolDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSessionProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 外部用户登录后同步到其他服务器中
 */
@InnerClientMessageHandler(100)
public class UserLoginSyncAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginSyncAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String username = decoder.readStr();
        String channelId = decoder.readStr();
        int userId = decoder.readInt();
        logger.info("用户登录同步, username:{}, userId:{}, channelId:{}", username, userId, channelId);
        ProtocolDispatcher.channels.stream()
                .filter(channel -> channel.id().asLongText().equals(channelId))
                .findFirst()
                .ifPresent(channel -> {
                    logger.info("用户登录同步2, username:{}, userId:{}, channelId:{}", username, userId, channelId);
                    outerUserSessionManager.loginWithWebsocketAndSyncAllServer(channel, (long) userId);
                    //通知客户端登录成功
                    HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                            .msgType(100).writeStr(username).writeLong(userId);
                    outerUserSessionManager.sendMsg2User(UserSessionProtocol.binary_websocket, builder, (long) userId);
                });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readStr();
    }
}
