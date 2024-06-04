package com.hbsoo.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

public class HttpClient {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                                    System.out.println("HTTP Response: " + msg.content().toString(io.netty.util.CharsetUtil.UTF_8));
                                    ctx.close();
                                }
                            });
                        }
                    });

            Channel ch = b.connect("localhost", 8080).sync().channel();

            FullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
            request.headers().set(HttpHeaderNames.HOST, "localhost");
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

            ch.writeAndFlush(request).sync();
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}