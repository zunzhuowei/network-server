package com.hbsoo.server.action.server;

import com.google.gson.Gson;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.session.ServerType;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收外部服务器登录后，同步用户会话信息到本服务器保存
 * Created by zun.wei on 2024/6/6.
 */
@InnerServerMessageHandler(HBSMessageType.InnerMessageType.LOGIN_SYNC)
public class InnerServerUserSessionLoginSyncAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerUserSessionLoginSyncAction.class);
    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long id = decoder.readLong();
        String username = decoder.readStr();
        String token = decoder.readStr();
        final int belongServerId = decoder.readInt();
        String belongServerHost = decoder.readStr();
        final int belongServerPort = decoder.readInt();
        final String belongServerType = decoder.readStr();
        logger.debug("InnerServerUserSessionLoginSyncAction id:{} username:{} token:{} belongServerId:{} belongServerHost:{} belongServerPort:{} belongServerType:{}",
                id, username, token, belongServerId, belongServerHost, belongServerPort, belongServerType);
        UserSession userSession = new UserSession();
        userSession.setId(id);
        userSession.setName(username);
        userSession.setToken(token);
        //userSession.setChannel(ctx.channel());
        final ServerInfo belongServer = new ServerInfo();
        belongServer.setId(belongServerId);
        belongServer.setHost(belongServerHost);
        belongServer.setPort(belongServerPort);
        belongServer.setType(ServerType.valueOf(belongServerType));
        userSession.setBelongServer(belongServer);
        outerSessionManager.login(id, userSession);

        //TODO测试发送消息给用户端
        //final ServerInfo serverInfo = NowServer.getServerInfo();
        //Gson gson = new Gson();
        //final String s = gson.toJson(serverInfo);
        //outerSessionManager.sendTextWebSocketFrameMsg2User(s, id);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.skipGetLong(HBSPackage.DecodeSkip.INT);
    }
}