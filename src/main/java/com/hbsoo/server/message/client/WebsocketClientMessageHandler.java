package com.hbsoo.server.message.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class WebsocketClientMessageHandler implements ClientMessageHandler<WebSocketFrame> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame msg) {
        final String s = msg.toString();
        System.out.println("WebsocketClientMessageHandler = " + s);
        ctx.close();
    }

}
