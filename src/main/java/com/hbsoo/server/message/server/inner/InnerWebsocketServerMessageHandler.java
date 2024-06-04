package com.hbsoo.server.message.server.inner;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerWebsocketServerMessageHandler implements InnerServerMessageHandler<WebSocketFrame> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {
        final String s = msg.toString();
        System.out.println("WebsocketMessageHandler = " + s);
        ctx.close();
    }

}
