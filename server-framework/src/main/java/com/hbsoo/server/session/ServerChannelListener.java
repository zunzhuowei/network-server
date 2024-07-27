package com.hbsoo.server.session;

import io.netty.channel.Channel;

/**
 * Created by zun.wei on 2024/7/27.
 */
public interface ServerChannelListener {

    default void onChannelActive(Channel channel){}
    default void onChannelInactive(Channel channel){}
    default void onUdpChannelActive(String senderHost, int senderPort) {}
    default void onUdpChannelInactive(String senderHost, int senderPort) {}
}
