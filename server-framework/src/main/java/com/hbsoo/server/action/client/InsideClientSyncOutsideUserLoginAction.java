package com.hbsoo.server.action.client;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * 同步网关登录的外部用户到本服务器,
 * 服务器登录时，接收已登录服务端推送已在服务端登录的用户；【网关在线-服务刚上线】
 * Created by zun.wei on 2024/6/6.
 */
@InsideClientMessageHandler(MessageType.Inside.LOGIN_SYNC)
public class InsideClientSyncOutsideUserLoginAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideClientSyncOutsideUserLoginAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long id = decoder.readLong();
        final int belongServerId = decoder.readInt();
        String belongServerHost = decoder.readStr();
        final int belongServerPort = decoder.readInt();
        final String belongServerType = decoder.readStr();
        String permissionStr = decoder.readStr();
        logger.debug("InsideClientSyncOutsideUserLoginAction id:{} belongServerId:{} belongServerHost:{} belongServerPort:{} belongServerType:{},permissionStr:{}",
                id, belongServerId, belongServerHost, belongServerPort, belongServerType, permissionStr);
        UserSession userSession = new UserSession();
        userSession.setId(id);
        final ServerInfo belongServer = new ServerInfo();
        belongServer.setId(belongServerId);
        belongServer.setHost(belongServerHost);
        belongServer.setPort(belongServerPort);
        belongServer.setType(belongServerType);
        userSession.setBelongServer(belongServer);
        Gson gson = new Gson();
        Set<String> set = gson.fromJson(permissionStr, Set.class);
        userSession.getPermissions().addAll(set);
        outsideUserSessionManager.login(id, userSession);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.skipGetLong(NetworkPacket.DecodeSkip.INT);
    }
}
