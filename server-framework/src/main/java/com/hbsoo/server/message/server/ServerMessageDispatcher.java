package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/13.
 */
public abstract class ServerMessageDispatcher implements ServerMessageHandler {

    /**
     * 处理消息，tcp, udp, websocket
     */
    public abstract void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 处理消息，http
     */
    public abstract void handle(ChannelHandlerContext ctx, HttpPackage httpPackage);

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }


}
