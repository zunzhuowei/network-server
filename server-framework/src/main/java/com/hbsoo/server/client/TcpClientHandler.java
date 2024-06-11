package com.hbsoo.server.client;

import com.hbsoo.server.message.client.InnerTcpClientMessageDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final InnerTcpClientMessageDispatcher dispatcher;

    public TcpClientHandler(InnerTcpClientMessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //byte[] received = new byte[in.readableBytes()];
        //in.readBytes(received);
        //System.out.println("Received: " + new String(received));
        dispatcher.onMessage(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}