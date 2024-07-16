package com.hbsoo.gateway.action;

import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.ChannelManager;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
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
        ExtendBody extendBody = decoder.readExtendBody();
        OutsideUserProtocol protocol = extendBody.getOutsideUserProtocol();
        String channelId = extendBody.getUserChannelId();
        String senderHost = extendBody.getSenderHost();
        int senderPort = extendBody.getSenderPort();
        String username = decoder.readStr();
        String roomName = decoder.readStr();
        int userId = decoder.readInt();
        String permissions = decoder.readStr();
        logger.info("用户登录同步, username:{}, userId:{}, channelId:{}", username, userId, channelId);
        if (protocol == OutsideUserProtocol.UDP) {
            ChannelManager.getUdpChannel(senderHost, senderPort)
                    .ifPresent(channel -> {
                        logger.info("udp login success, username:{}, userId:{}, senderHost:{}, senderPort:{}", username, userId, senderHost, senderPort);
                        outsideUserSessionManager.loginWithUdpAndSyncAllServer(channel, (long) userId, senderHost, senderPort, permissions);
                        //通知客户端登录成功
                        NetworkPacket.Builder builder = NetworkPacket.Builder
                                .withHeader(NetworkPacket.UDP_HEADER)
                                .msgType(100).writeStr(username).writeLong(userId);
                        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.UDP, builder, (long) userId);
                    });
            return;
        }
        if (protocol == OutsideUserProtocol.TCP) {
            ChannelManager.getChannel(channelId)
                    .ifPresent(channel -> {
                        logger.info("tcp login success, username:{}, userId:{}, channelId:{}", username, userId, channelId);
                        outsideUserSessionManager.loginWithTcpAndSyncAllServer(channel, (long) userId, permissions);
                        //通知客户端登录成功
                        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                                .msgType(100).writeStr(username).writeLong(userId);
                        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.TCP, builder, (long) userId);
                    });
            return;
        }
        ChannelManager.getChannel(channelId)
                .ifPresent(channel -> {
                    logger.info("websocket login success, username:{}, userId:{}, channelId:{}", username, userId, channelId);
                    outsideUserSessionManager.loginWithBinWebsocketAndSyncAllServer(channel, (long) userId, permissions);
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
