package com.hbsoo.server.action.server;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * 同步网关登录的外部用户到本服务器，
 * 实时接收用户登录消息；【网关在线-服务也在线】
 * Created by zun.wei on 2024/6/6.
 */
@InsideServerMessageHandler(MessageType.Inside.LOGIN_SYNC)
public class InsideServerSyncOutsideUserLoginAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerSyncOutsideUserLoginAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long id = decoder.readLong();
        int belongServerId = decoder.readInt();
        String belongServerHost = decoder.readStr();
        int belongServerPort = decoder.readInt();
        String belongServerType = decoder.readStr();
        String permissionStr = decoder.readStr();
        String channelId = decoder.readStr();
        byte protocolType = decoder.readByte();
        logger.debug("id:{} belongServerId:{} belongServerHost:{} belongServerPort:{} belongServerType:{},permissionStr:{}",
                id, belongServerId, belongServerHost, belongServerPort, belongServerType, permissionStr);
        UserSession userSession = new UserSession();
        userSession.setId(id);
        ServerInfo belongServer = new ServerInfo();
        belongServer.setId(belongServerId);
        belongServer.setHost(belongServerHost);
        belongServer.setPort(belongServerPort);
        belongServer.setType(belongServerType);
        userSession.setBelongServer(belongServer);
        Gson gson = new Gson();
        Set<String> set = gson.fromJson(permissionStr, Set.class);
        userSession.getPermissions().addAll(set);
        userSession.setChannelId(channelId);
        userSession.setProtocolType(protocolType);
        if (OutsideUserProtocol.getProtocol(protocolType) == OutsideUserProtocol.UDP) {
            String senderHost = decoder.readStr();
            int senderPort = decoder.readInt();
            userSession.setUdpHost(senderHost);
            userSession.setUdpPort(senderPort);
        }
        outsideUserSessionManager.login(id, userSession);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.skipGetLong();
    }
}
