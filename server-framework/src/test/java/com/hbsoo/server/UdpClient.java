package com.hbsoo.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

public class UdpClient {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                                    ByteBuf content = packet.content();
                                    byte[] received = new byte[content.readableBytes()];
                                    content.readBytes(received);
                                    System.out.println("UDP Response: " + new String(received));
                                }
                            });
                        }
                    });

            Channel ch = b.bind("localhost", 8080).sync().channel();
            ByteBuf buf = Unpooled.copiedBuffer("Hello, UDP Server!".getBytes());
            ch.writeAndFlush(new DatagramPacket(buf, new InetSocketAddress("localhost", 8080))).sync();
            ch.closeFuture().await(10000); // wait for 10 seconds for responses
        } finally {
            group.shutdownGracefully();
        }
    }
}
