package com.hbsoo.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TcpClient {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    byte[] received = new byte[msg.readableBytes()];
                                    msg.readBytes(received);
                                    System.out.println("TCP Response: " + new String(received));
                                }
                            });
                        }
                    });

            Channel ch = b.connect("localhost", 5555).sync().channel();
            //ByteBuf buf = Unpooled.copiedBuffer("THBSHello, TCP Server!".getBytes());
            //ch.writeAndFlush(buf).sync();
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
