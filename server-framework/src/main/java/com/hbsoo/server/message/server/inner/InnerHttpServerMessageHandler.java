package com.hbsoo.server.message.server.inner;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerHttpServerMessageHandler implements InnerServerMessageHandler<FullHttpRequest> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpRequest msg) {
        final String s = msg.toString();
        System.out.println("HttpMessageHandler = " + s);
        ctx.close();
    }

}
