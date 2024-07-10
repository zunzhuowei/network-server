package com.hbsoo.server.client;

import com.hbsoo.server.config.ServerInfo;
import io.netty.channel.ChannelFuture;

/**
 * Created by zun.wei on 2024/6/27.
 */
public interface InsideTcpClientConnectListener {

    /**
     * 连接成功
     * @param channelFuture 连接回调
     * @param fromServerInfo 来自服务器信息
     * @param toServerInfo 到服务器信息
     * @param index 客户端编号
     */
    void onConnectSuccess(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index);

    /**
     * 连接失败
     * @param channelFuture 连接回调
     * @param fromServerInfo 来自服务器信息
     * @param toServerInfo 到服务器信息
     * @param index 客户端编号
     */
    void onConnectFail(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index);

}
