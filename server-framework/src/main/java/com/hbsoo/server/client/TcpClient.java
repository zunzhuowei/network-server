package com.hbsoo.server.client;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerClientMessageDispatcher;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * 内部服务，链接客户端，用于内部服务之间的通信
 *
 */
public final class TcpClient {

    @Autowired
    private InnerClientMessageDispatcher innerClientMessageDispatcher;
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
            return new Thread(r, "client-" + toServerInfo.getType() + "-" + toServerInfo.getId() + "#" + index);
        });
    }

    public void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 每隔三秒发送一次心跳
                        pipeline.addLast(new IdleStateHandler(0, 0, 3, TimeUnit.SECONDS));
                        pipeline.addLast(new HeartbeatHandler(bootstrap, b -> connect(b)));
                        pipeline.addLast(new LengthFieldBasedFrameDecoder
                                (1024 * 1024, 4, 4, 0, 0));
                        pipeline.addLast(new TcpClientHandler(innerClientMessageDispatcher));
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
            if (future.isSuccess()) {
                //System.out.println("Connected to server");
                final Channel channel = future.channel();
                // 登录消息
                byte[] aPackage = HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.InnerMessageType.LOGIN)
                        .writeInt(fromServerInfo.getId())//当前服务器的ID
                        .writeStr(fromServerInfo.getType())//当前服务器的类型
                        .writeInt(index)//客户端编号
                        .writeInt(toServerInfo.getId())//登录服务器的ID
                        .writeStr(toServerInfo.getType())//登录服务器的类型
                        .buildPackage();
                ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
                channel.writeAndFlush(buf).sync();
            } else {
                System.out.println("【" + fromServerInfo.getType() + "】 Client #" + index
                        + " Failed to connect to server 【" + toServerInfo.getType() + "】"
                        + toServerInfo.getHost() + ":" + toServerInfo.getPort()
                        + ", trying to reconnect in " + reconnectInterval + " seconds");
                // 延迟重连
                future.channel().eventLoop().schedule(() -> connect(bootstrap), reconnectInterval, TimeUnit.SECONDS);
            }
        });
    }

}
