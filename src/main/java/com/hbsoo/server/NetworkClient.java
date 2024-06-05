package com.hbsoo.server;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.TcpClientMessageHandler;
import com.hbsoo.server.session.ServerType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 链接内部服务器的客户端
 * Created by zun.wei on 2024/6/4.
 */
public final class NetworkClient {

    private final TcpClientMessageHandler handlers;
    private EventLoopGroup[] groups;
    private final List<ServerInfo> innerServers;
    private final Integer serverId;
    private final ServerType serverType;
    public static Map<ServerType, ConcurrentHashMap<Integer, Channel>> clients = new ConcurrentHashMap<>();

    public NetworkClient(List<ServerInfo> innerServers, TcpClientMessageHandler tcpClientMessageHandler,
                         Integer serverId, ServerType serverType) {
        for (ServerInfo innerClient : innerServers) {
            ConcurrentHashMap<Integer, Channel> networkClients = clients.get(innerClient.getType());
            if (networkClients == null) {
                networkClients = new ConcurrentHashMap<>();
                clients.put(innerClient.getType(), networkClients);
            }
        }
        this.handlers = tcpClientMessageHandler;
        this.innerServers = innerServers;
        this.serverId = serverId;
        this.serverType = serverType;
    }


    public void connect() {
        groups = new NioEventLoopGroup[innerServers.size()];
        for (int i = 0; i < innerServers.size(); i++) {
            final Integer id = innerServers.get(i).getId();
            if (Objects.equals(id, serverId)) {
                continue;
            }
            final String host = innerServers.get(i).getHost();
            final int port = innerServers.get(i).getPort();
            final ServerType type = innerServers.get(i).getType();
            EventLoopGroup group = groups[i] = new NioEventLoopGroup();
            new Thread(() -> {
                try {
                    start(host, port, type, group, id);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void start(String host, int port, ServerType serverType, EventLoopGroup group, Integer id) throws InterruptedException {
        Channel channel = null;
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
                                    //byte[] received = new byte[msg.readableBytes()];
                                    //msg.readBytes(received);
                                    //System.out.println("TCP Response: " + new String(received));
                                    handlers.onMessage(ctx, msg);
                                }
                            });
                        }
                    });
            channel = b.connect(host, port).sync().channel();
            // 登录消息
            byte[] aPackage = HBSPackage.Builder.withDefaultHeader()
                    .writeInt(HBSMessageType.InnerMessageType.LOGIN)
                    .writeInt(serverId)//当前服务器的ID
                    .writeStr(this.serverType.name())//当前服务器的类型
                    .writeInt(id)//登录服务器的ID
                    .buildPackage();
            ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
            channel.writeAndFlush(buf).sync();
            // 保存客户端管道
            clients.get(serverType).put(id, channel);
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            if (Objects.nonNull(channel)) {
                channel.close();
            }
            clients.get(serverType).remove(id);
            TimeUnit.SECONDS.sleep(5);
            start(host, port, serverType, group, id);
        }
    }


    public void stop() {
        for (int i = 0; i < groups.length; i++) {
            groups[i].shutdownGracefully();
        }
        clients.forEach((k, v) -> v.forEach((k1, v1) -> v1.close()));
    }


}
