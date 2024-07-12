package com.hbsoo.gateway.action;

import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.message.entity.ExpandBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.netty.ProtocolDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 外部用户登录后同步到其他服务器中
 */
@InsideClientMessageHandler(100)
public class UserLoginSyncAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginSyncAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExpandBody expandBody = decoder.readExpandBody();
        String channelId = expandBody.getUserChannelId();
        String username = decoder.readStr();
        int userId = decoder.readInt();
        String permissions = decoder.readStr();
        logger.info("用户登录同步, username:{}, userId:{}, channelId:{}", username, userId, channelId);
        ProtocolDispatcher.channels.stream()
                .filter(channel -> channel.id().asLongText().equals(channelId))
                .findFirst()
                .ifPresent(channel -> {
                    logger.info("用户登录同步2, username:{}, userId:{}, channelId:{}", username, userId, channelId);
                    outsideUserSessionManager.loginWithWebsocketAndSyncAllServer(channel, (long) userId, permissions);
                    //通知客户端登录成功
                    NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                            .msgType(100).writeStr(username).writeLong(userId);
                    outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, (long) userId);
                });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
