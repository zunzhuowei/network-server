package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class WebsocketServerMessageDispatcher implements ServerMessageHandler<WebSocketFrame>{

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
