package com.hbsoo.server;

import com.hbsoo.server.message.server.ServerMessageHandler;
import com.hbsoo.server.message.server.ProtocolDispatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 网络服务器，可用于内部服务也可以用于外部服务
 * Created by zun.wei on 2024/5/31.
 */
public final class NetworkServer {

    private ServerMessageHandler[] handlers;
    private int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NetworkServer(int port, ServerMessageHandler... serverMessageHandlers) {
        this.port = port;
        this.handlers = serverMessageHandlers;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ProtocolDispatcher(handlers));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        serverChannel = b.bind(port).sync().channel();
        System.out.println("Netty server started on port " + port);
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
