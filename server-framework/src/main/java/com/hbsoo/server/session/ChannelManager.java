package com.hbsoo.server.session;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

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
    }

    public static void removeChannel(Channel channel) {
        channelMap.remove(channel.id().asLongText());
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
        //channelMap.put(senderHost + ":" + senderPort, channel);
    }

    /**
     * 针对UDP 协议
     */
    public static void removeUdpChannel(String senderHost, int senderPort) {
        channelMap.remove(senderHost + ":" + senderPort);
    }

    public static Optional<Channel> getUdpChannel(String senderHost, int senderPort) {
        return Optional.ofNullable(channelMap.get(senderHost + ":" + senderPort));
    }
}

