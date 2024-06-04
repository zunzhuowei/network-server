package com.hbsoo.server.message.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class TcpClientMessageHandler implements ClientMessageHandler<ByteBuf> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        final String s = msg.toString();
        System.out.println("TcpClientMessageHandler = " + s);
        ctx.close();
    }
}
