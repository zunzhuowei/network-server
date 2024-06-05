package com.hbsoo.server.message.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerHttpClientMessageHandler implements InnerClientMessageHandler<FullHttpResponse> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpResponse msg) {
        final String s = msg.toString();
        System.out.println("HttpClientMessageHandler = " + s);
        ctx.close();
    }

}
