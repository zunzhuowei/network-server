package com.hbsoo.server.action.client.outer;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.action.server.InnerServerHeartbeatAction;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.client.outer.WebsocketOuterUserLoginAuthenticator;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/12.
 */
@OuterServerMessageHandler(HBSMessageType.OuterMessageType.LOGIN)
public class OuterWebsocketUserLoginAction extends ServerMessageDispatcher {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OuterSessionManager outerSessionManager;
    @Autowired(required = false)
    private WebsocketOuterUserLoginAuthenticator websocketOuterUserLoginAuthenticator;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        if (Objects.isNull(websocketOuterUserLoginAuthenticator)) {
            logger.warn("websocketOuterUserLoginAuthenticator is null");
            return;
        }
        Long userId = websocketOuterUserLoginAuthenticator.authentication(ctx, decoder);
        if (Objects.isNull(userId)) {
            logger.debug("websocketOuterUserLoginAuthenticator authentication is null");
            return;
        }
        UserSession userSession = new UserSession();
        userSession.setId(userId);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        userSession.setUdp(false);
        outerSessionManager.loginAndSyncAllServer(userSession.getId(), userSession);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        if (ctx.channel() instanceof NioDatagramChannel) {
            String sendHost = decoder.readStr();
            int sendPort = decoder.readInt();
            return sendHost + ":" + sendPort;
        }
        return ctx.channel().id().asShortText();
    }
}
