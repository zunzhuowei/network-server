package com.hbsoo.server;

import com.hbsoo.server.netty.HttpRequestHandler;
import com.hbsoo.server.netty.TcpServerHandler;
import com.hbsoo.server.netty.UdpServerHandler;
import com.hbsoo.server.netty.WebSocketFrameHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebServer {
    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new WebServer(port).start();
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventLoopGroup udpGroup = new NioEventLoopGroup();
        try {
            // Start TCP server
            ServerBootstrap tcpBootstrap = new ServerBootstrap();
            tcpBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpServerHandler(null));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start HTTP/WebSocket server
            ServerBootstrap httpWsBootstrap = new ServerBootstrap();
            httpWsBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpRequestHandler(null));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                            ch.pipeline().addLast(new WebSocketFrameHandler(null));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start UDP server
            Bootstrap udpBootstrap = new Bootstrap();
            udpBootstrap.group(udpGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        public void initChannel(DatagramChannel ch) {
                            ch.pipeline().addLast(new UdpServerHandler(null));
                        }
                    });

            ChannelFuture tcpFuture = tcpBootstrap.bind(port).sync();
            ChannelFuture httpWsFuture = httpWsBootstrap.bind(port + 1).sync();
            ChannelFuture udpFuture = udpBootstrap.bind(port + 2).sync();

            System.out.println("TCP server started on port " + port);
            System.out.println("HTTP/WebSocket server started on port " + (port + 1));
            System.out.println("UDP server started on port " + (port + 2));

            tcpFuture.channel().closeFuture().sync();
            httpWsFuture.channel().closeFuture().sync();
            udpFuture.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            udpGroup.shutdownGracefully();
        }
    }
}