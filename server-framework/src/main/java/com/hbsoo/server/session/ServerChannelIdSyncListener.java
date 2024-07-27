package com.hbsoo.server.session;

/**
 * Created by zun.wei on 2024/7/27.
 */
public interface ServerChannelIdSyncListener {

    default void onChannelActive(String channelLongId){}
    default void onChannelInactive(String channelLongId){}
    default void onUdpChannelActive(String senderHost, int senderPort) {}
    default void onUdpChannelInactive(String senderHost, int senderPort) {}
}
