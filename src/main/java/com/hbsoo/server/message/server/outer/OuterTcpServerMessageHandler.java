package com.hbsoo.server.message.server.outer;

import com.hbsoo.server.message.server.inner.InnerServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class OuterTcpServerMessageHandler implements OuterServerMessageHandler<ByteBuf> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        final String s = msg.toString();
        System.out.println("TcpMessageHandler = " + s);
        ctx.close();
    }
}
