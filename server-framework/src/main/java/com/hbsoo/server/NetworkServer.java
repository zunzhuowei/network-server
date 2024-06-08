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

import java.util.concurrent.ThreadFactory;

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

    /**
     * BossGroup：
     * BossGroup通常只需要1到2个线程，因为它的主要任务是接收新的连接请求，然后将这些请求分配给WorkerGroup。
     * 在大多数情况下，一个线程就足够了，除非你预期会有非常高并发的连接请求。
     * WorkerGroup：
     * WorkerGroup的线程数通常设置为CPU核心数的2倍或3倍，因为它们处理连接后的实际I/O操作和业务逻辑。
     * 这允许在多核系统上充分利用硬件资源，同时减少线程间的上下文切换。
     * 如果你的应用是计算密集型的，可以减少WorkerGroup的线程数，避免过多线程导致的上下文切换开销。
     * 如果应用是I/O密集型，可以适当增加线程数，以便处理更多的并发连接。
     */
    public void start() throws Exception {
        int bossThreadCount = 1; // 通常为1
        int workerThreadCount = Runtime.getRuntime().availableProcessors() * 2; // 可以根据实际情况调整
        bossGroup = new NioEventLoopGroup(bossThreadCount, r -> {
            return new Thread(r, "Netty-Boss-" + r.hashCode());
        });
        workerGroup = new NioEventLoopGroup(workerThreadCount, r -> {
            return new Thread(r, "Netty-Worker-" + r.hashCode());
        });
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
