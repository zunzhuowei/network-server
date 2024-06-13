package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.util.Objects;

public final class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final ServerMessageHandler handler;

    public UdpServerHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        /*ByteBuf content = packet.content();
        byte[] received = new byte[content.readableBytes()];
        content.readBytes(received);
        String message = new String(received);
        System.out.println("Received UDP message: " + message);
        ByteBuf response = ctx.alloc().buffer().writeBytes(("Echo: " + message).getBytes());
        ctx.writeAndFlush(new DatagramPacket(response, packet.sender()));*/
        handler.onMessage(ctx, packet);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("UdpServerHandler channelInactive");
        ctx.close();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("UdpServerHandler channelActive");
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("UdpServerHandler channelRegistered");
    }
}