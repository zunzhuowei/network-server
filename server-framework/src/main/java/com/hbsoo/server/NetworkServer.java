package com.hbsoo.server;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageHandler;
import com.hbsoo.server.netty.ProtocolDispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Set;

/**
 * 网络服务器，可用于内部服务也可以用于外部服务
 * Created by zun.wei on 2024/5/31.
 */
public final class NetworkServer {

    private final ServerMessageHandler handler;
    private final int port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final int maxFrameLength;
    private final String serverName;
    private final Set<String> protocols;

    public NetworkServer(String serverName, int port, int maxFrameLength, ServerMessageHandler handler, Set<String> protocols) {
        this.port = port;
        this.handler = handler;
        this.maxFrameLength = maxFrameLength;
        int bossThreadCount = 1; // 通常为1
        int workerThreadCount = Runtime.getRuntime().availableProcessors() * 2; // 可以根据实际情况调整
        this.serverName = serverName;
        this.protocols = protocols;
        bossGroup = new NioEventLoopGroup(bossThreadCount, r -> {
            return new Thread(r, serverName + "-Boss-" + port);
        });
        workerGroup = new NioEventLoopGroup(workerThreadCount, r -> {
            return new Thread(r, serverName + "-Worker-" + port);
        });
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
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ProtocolDispatcher(handler, maxFrameLength, protocols));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        if (protocols.contains("UDP")) {
            enableUdpServer();
        }
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

    /**
     * 启动UDP服务
     */
    private void enableUdpServer() {
        //TODO因为使用Bootstrap，所以在异常关闭channel的时候，后续客户端再发来消息就收不到了
        Thread udpThread = new Thread(() -> {
            try {
                Bootstrap udpBootstrap = new Bootstrap();
                udpBootstrap.group(workerGroup)
                        // 设置使用NioDatagramChannel作为服务器的通道实现
                        .channel(NioDatagramChannel.class)
                        // 设置日志级别
                        .handler(new LoggingHandler(LogLevel.INFO))
                        // 使用匿名内部类初始化通道
                        .handler(new ProtocolDispatcher(handler, maxFrameLength, protocols))
                        //.handler(new LengthFieldBasedFrameDecoder
                        //        (maxFrameLength, 4, 4, 0, 0))
                        //.handler(new UdpServerHandler(handler))
                        .option(ChannelOption.SO_BROADCAST, true)
                        .option(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        .option(ChannelOption.SO_SNDBUF, 5 * 1024 * 1024)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65525))
                ;

                // 绑定一个端口并且同步，生成了一个ChannelFuture对象
                ChannelFuture future = udpBootstrap.bind(port).sync();
                if (future.isSuccess()) {
                    System.out.println("UDP server started on port " + port);
                    //防止客户端第一个包丢失问题，自己给自己先发一个
                    HBSPackage.Builder.withHeader(HBSPackage.UDP_HEADER)
                            .msgType(0).buildAndSendUdpTo(future.channel(), "127.0.0.1", port);
                } else {
                    System.out.println("UDP server failed to start on port " + port);
                }
                future.channel().closeFuture().addListener(future1 -> {
                    if (future1.isSuccess()) {
                        System.out.println("UDP server closed successfully.");
                    } else {
                        System.out.println("UDP server closed with error: " + future1.cause());
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        udpThread.setDaemon(true);
        udpThread.setName(serverName + "-UDP-" + port);
        udpThread.start();
    }
}
