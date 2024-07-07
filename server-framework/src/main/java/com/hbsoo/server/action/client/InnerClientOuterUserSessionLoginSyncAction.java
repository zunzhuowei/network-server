package com.hbsoo.server.action.client;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 同步网关登录的外部用户到本服务器
 * Created by zun.wei on 2024/6/6.
 */
@InnerClientMessageHandler(HBSMessageType.Inner.LOGIN_SYNC)
public class InnerClientOuterUserSessionLoginSyncAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientOuterUserSessionLoginSyncAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long id = decoder.readLong();
        final int belongServerId = decoder.readInt();
        String belongServerHost = decoder.readStr();
        final int belongServerPort = decoder.readInt();
        final String belongServerType = decoder.readStr();
        logger.debug("InnerClientOuterUserSessionLoginSyncAction id:{} belongServerId:{} belongServerHost:{} belongServerPort:{} belongServerType:{}",
                id, belongServerId, belongServerHost, belongServerPort, belongServerType);
        UserSession userSession = new UserSession();
        userSession.setId(id);
        final ServerInfo belongServer = new ServerInfo();
        belongServer.setId(belongServerId);
        belongServer.setHost(belongServerHost);
        belongServer.setPort(belongServerPort);
        belongServer.setType(belongServerType);
        userSession.setBelongServer(belongServer);
        outerUserSessionManager.login(id, userSession);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.skipGetLong(HBSPackage.DecodeSkip.INT);
    }
}
