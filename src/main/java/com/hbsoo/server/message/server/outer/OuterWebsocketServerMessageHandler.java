package com.hbsoo.server.message.server.outer;

import com.hbsoo.server.message.server.inner.InnerServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class OuterWebsocketServerMessageHandler implements OuterServerMessageHandler<WebSocketFrame> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {
        final String s = msg.toString();
        System.out.println("WebsocketMessageHandler = " + s);
        ctx.close();
    }

}
