package com.hbsoo.server.message.server.inner;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerTcpServerMessageHandler implements InnerServerMessageHandler<ByteBuf> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        final String s = msg.toString();
        System.out.println("TcpMessageHandler = " + s);
        ctx.close();
    }
}
