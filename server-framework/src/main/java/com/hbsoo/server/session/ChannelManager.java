package com.hbsoo.server.session;

import com.google.gson.Gson;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每一个连接服务器的客户端管道都可以在这里找到，包括内部服务和外部服务的客户端；
 */
public final class ChannelManager {

    private static final ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();
    //private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static void addChannel(Channel channel) {
        channelMap.put(channel.id().asLongText(), channel);
        Map<String, ServerChannelListener> beans = SpringBeanFactory.getBeansOfType(ServerChannelListener.class);
        beans.values().forEach(listener -> {
            listener.onChannelActive(channel);
        });
        sync2AllServer(false, true, channel.id().asLongText());
    }

    public static void removeChannel(Channel channel) {
        channelMap.remove(channel.id().asLongText());
        Map<String, ServerChannelListener> beans = SpringBeanFactory.getBeansOfType(ServerChannelListener.class);
        beans.values().forEach(listener -> {
            listener.onChannelInactive(channel);
        });
        sync2AllServer(false, false, channel.id().asLongText());
    }

    public static Optional<Channel> getChannel(String channelId) {
        return Optional.ofNullable(channelMap.get(channelId));
    }

    public static void closeAllChannels() {
        try {
            for (Channel channel : channelMap.values()) {
                try {
                    channel.close();
                } catch (Exception e) {
                   // e.printStackTrace();
                }
            }
        } finally {
            channelMap.clear();
        }
    }


    /**
     * 针对UDP 协议
     */
    public static void addUdpChannel(String senderHost, int senderPort, Channel channel) {
        channelMap.putIfAbsent(senderHost + ":" + senderPort, channel);
        Map<String, ServerChannelListener> beans = SpringBeanFactory.getBeansOfType(ServerChannelListener.class);
        beans.values().forEach(listener -> {
            listener.onUdpChannelActive(senderHost, senderPort);
        });
        sync2AllServer(true, true, senderHost + ":" + senderPort);
    }

    /**
     * 针对UDP 协议
     */
    public static void removeUdpChannel(String senderHost, int senderPort) {
        channelMap.remove(senderHost + ":" + senderPort);
        Map<String, ServerChannelListener> beans = SpringBeanFactory.getBeansOfType(ServerChannelListener.class);
        beans.values().forEach(listener -> {
            listener.onUdpChannelInactive(senderHost, senderPort);
        });
        sync2AllServer(true, false, senderHost + ":" + senderPort);
    }

    public static Optional<Channel> getUdpChannel(String senderHost, int senderPort) {
        return Optional.ofNullable(channelMap.get(senderHost + ":" + senderPort));
    }

    /**
     * 同步消息到其他服务
     * @param isUdp true: udp
     * @param active true: active
     * @param channelLongId channelLongId
     */
    private static void sync2AllServer(boolean isUdp, boolean active, String channelLongId) {
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.CHANNEL_ID_SYNC)
                .writeBoolean(isUdp)
                .writeBoolean(active)
                .writeStr(channelLongId);
        InsideClientSessionManager.forwardMsg2AllServerByKeyUseSender(builder, channelLongId);
    }

}

