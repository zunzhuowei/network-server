package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.ServerChannelIdSyncListener;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

/**
 * Created by zun.wei on 2024/7/27.
 */
@InsideServerMessageHandler(value = MessageType.Inside.CHANNEL_ID_SYNC, protocol = Protocol.TCP)
public class ChannelIdSyncAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        boolean isUdp = decoder.readBoolean();
        boolean isActive = decoder.readBoolean();
        String channelLongId = decoder.readStr();
        Map<String, ServerChannelIdSyncListener> beans = SpringBeanFactory.getBeansOfType(ServerChannelIdSyncListener.class);
        if (isActive) {
            if (isUdp) {
                String[] hostPort = channelLongId.split(":");
                String senderHost = hostPort[0];
                int senderPort = Integer.parseInt(hostPort[1]);
                beans.values().forEach(listener -> listener.onUdpChannelActive(senderHost, senderPort));
            } else {
                beans.values().forEach(listener -> listener.onChannelActive(channelLongId));
            }
        } else {
            if (isUdp) {
                String[] hostPort = channelLongId.split(":");
                String senderHost = hostPort[0];
                int senderPort = Integer.parseInt(hostPort[1]);
                beans.values().forEach(listener -> listener.onUdpChannelInactive(senderHost, senderPort));
            } else {
                beans.values().forEach(listener -> listener.onChannelInactive(channelLongId));
            }
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
