package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class HttpServerMessageDispatcher implements ServerMessageHandler<FullHttpRequest>{

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpRequest msg) {
        final String s = msg.toString();
        System.out.println("HttpMessageHandler = " + s);
        ctx.close();
    }

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
