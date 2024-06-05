package com.hbsoo.server.message.server;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public interface ServerMessageHandler<T> {


    void onMessage(ChannelHandlerContext ctx, T msg);


}
