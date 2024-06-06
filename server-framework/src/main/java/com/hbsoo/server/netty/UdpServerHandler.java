package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.util.Objects;

public final class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final ServerMessageHandler udpServerMessageDispatcher;

    public UdpServerHandler(ServerMessageHandler udpServerMessageDispatcher) {
        this.udpServerMessageDispatcher = udpServerMessageDispatcher;
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
        if (Objects.nonNull(udpServerMessageDispatcher)) {
            udpServerMessageDispatcher.onMessage(ctx, packet);
        } else {
            final String s = packet.toString();
            System.err.println("UdpServerHandler not config = " + s);
            ctx.close();
        }
    }
}