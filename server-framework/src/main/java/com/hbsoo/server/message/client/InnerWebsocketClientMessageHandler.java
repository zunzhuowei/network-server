package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerWebsocketClientMessageHandler implements InnerClientMessageHandler<WebSocketFrame> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        final ByteBuf msg = webSocketFrame.content();
        byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
        onMessage(ctx, decoder);
    }

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
