package com.hbsoo.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/5/31.
 */
public class UnknownProtocolHandler extends SimpleChannelInboundHandler<Object> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        final String s = o.toString();
        System.err.println("UnknownProtocolHandler = " + s);
        channelHandlerContext.close();
    }

}
