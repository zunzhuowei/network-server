package com.hbsoo.server.message.client;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public interface ClientMessageHandler<T> {


    void onMessage(ChannelHandlerContext ctx, T msg);


}
