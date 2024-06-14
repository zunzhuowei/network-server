package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HttpPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/14.
 */
public abstract class HttpServerMessageDispatcher implements ServerMessageHandler {


    /**
     * 处理消息，http
     */
    public abstract void handle(ChannelHandlerContext ctx, HttpPackage httpPackage);

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }

}
