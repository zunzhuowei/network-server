package com.hbsoo.server;

import com.hbsoo.server.message.entity.NetworkPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.DatagramPacket;

public class UdpClient {
    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    //super.channelActive(ctx);
                                }

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

            final ChannelFuture channelFuture = b.bind(0).sync();
            //ByteBuf buf = Unpooled.copiedBuffer("Hello, UDP Server!".getBytes());
            //channelFuture.channel().writeAndFlush(new DatagramPacket(buf, new InetSocketAddress("localhost", 5555))).sync();
            NetworkPacket.Builder.withHeader(NetworkPacket.UDP_HEADER)
                    .msgType(1).writeStr("Hello, UDP Server!")
                    .sendUdpTo(channelFuture.channel(), "localhost", 5555);
            NetworkPacket.Builder.withHeader(NetworkPacket.UDP_HEADER)
                    .msgType(1).writeStr("Hello, UDP Server2!")
                    .sendUdpTo(channelFuture.channel(), "localhost", 5555);
            NetworkPacket.Builder.withHeader(NetworkPacket.UDP_HEADER)
                    .msgType(1).writeStr("Hello, UDP Server3!")
                    .sendUdpTo(channelFuture.channel(), "localhost", 5555);
            channelFuture.channel().closeFuture().await(); // wait for 10 seconds for responses
        } finally {
            group.shutdownGracefully();
        }
    }
}
