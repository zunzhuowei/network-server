package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

public final class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ServerMessageHandler handler;


    public TcpServerHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        /*byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        String message = new String(received);
        System.out.println("Received TCP message: " + message);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(("Echo: " + message).getBytes()));*/
        handler.onMessage(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("TcpServerHandler channelInactive");
        ctx.close();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("TcpServerHandler channelActive");
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("TcpServerHandler channelRegistered");
    }
}
