package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/14.
 */
public abstract class HttpServerMessageDispatcher extends ServerMessageDispatcher {


    /**
     * 处理消息，http
     */
    public abstract void handle(ChannelHandlerContext ctx, HttpPackage httpPackage);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }

}
