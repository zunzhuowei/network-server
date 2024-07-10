package com.hbsoo.server.client;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.client.InsideClientMessageDispatcher;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 内部服务，链接客户端，用于内部服务之间的通信
 *
 */
public final class TcpClient {

    @Autowired
    private InsideClientMessageDispatcher insideClientMessageDispatcher;

    private final int reconnectInterval;
    private final ServerInfo fromServerInfo;
    private final ServerInfo toServerInfo;
    private final NioEventLoopGroup group;
    private final int index;

    public TcpClient(ServerInfo fromServerInfo, ServerInfo toServerInfo, int reconnectInterval, int index) {
        this.reconnectInterval = reconnectInterval;
        this.fromServerInfo = fromServerInfo;
        this.toServerInfo = toServerInfo;
        this.index = index;
        group = new NioEventLoopGroup(1, r -> {
            return new Thread(r, "IC2" + toServerInfo.getType() + ":" + toServerInfo.getId() + "#" + index);
        });
    }

    public void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                .option(ChannelOption.SO_SNDBUF, 5 * 1024 * 1024)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 每隔三秒发送一次心跳
                        pipeline.addLast(new IdleStateHandler(0, 0, 3, TimeUnit.SECONDS));
                        pipeline.addLast(new HeartbeatHandler(bootstrap, b -> connect(b)));
                        pipeline.addLast(new LengthFieldBasedFrameDecoder
                                (1024 * 1024, NetworkPacket.TCP_HEADER.length, 4, 0, 0));
                        pipeline.addLast(new TcpClientHandler(insideClientMessageDispatcher));
                    }
                });
        connect(bootstrap);
    }

    public void stop() {
        System.out.println("stop client");
        group.shutdownGracefully();
    }

    private void connect(Bootstrap bootstrap) {
        bootstrap.connect(toServerInfo.getHost(), toServerInfo.getPort()).addListener((ChannelFutureListener) future -> {
            Map<String, InsideTcpClientConnectListener> beans = SpringBeanFactory.getBeansOfType(InsideTcpClientConnectListener.class);
            if (future.isSuccess()) {
                beans.values().forEach(listener -> listener.onConnectSuccess(future, fromServerInfo, toServerInfo, index));
            } else {
                System.out.println("【" + fromServerInfo.getType() + "】 Client #" + index
                        + " Failed to connect to server 【" + toServerInfo.getType() + "】"
                        + toServerInfo.getHost() + ":" + toServerInfo.getPort()
                        + ", trying to reconnect in " + reconnectInterval + " seconds");
                // 延迟重连
                future.channel().eventLoop().schedule(() -> connect(bootstrap), reconnectInterval, TimeUnit.SECONDS);
                beans.values().forEach(listener -> listener.onConnectFail(future, fromServerInfo, toServerInfo, index));
            }
        });
    }

}
