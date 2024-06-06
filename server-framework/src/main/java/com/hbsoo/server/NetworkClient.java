package com.hbsoo.server;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerTcpClientMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.session.ServerType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 链接内部服务器的客户端
 * Created by zun.wei on 2024/6/4.
 */
public final class NetworkClient {

    private final InnerTcpClientMessageDispatcher dispatcher;
    private final List<ServerInfo> innerServers;
    private final Integer serverId;
    private final ServerType serverType;

    public NetworkClient(List<ServerInfo> innerServers, InnerTcpClientMessageDispatcher dispatcher,
                         Integer serverId, ServerType serverType) {
        for (ServerInfo innerClient : innerServers) {
            ConcurrentHashMap<Integer, Channel> networkClients = InnerClientSessionManager.clients.get(innerClient.getType());
            if (networkClients == null) {
                networkClients = new ConcurrentHashMap<>();
                InnerClientSessionManager.clients.put(innerClient.getType(), networkClients);
            }
        }
        this.dispatcher = dispatcher;
        this.innerServers = innerServers;
        this.serverId = serverId;
        this.serverType = serverType;
    }


    public void connect() {
        for (int i = 0; i < innerServers.size(); i++) {
            final Integer id = innerServers.get(i).getId();
            if (Objects.equals(id, serverId)) {
                continue;
            }
            final String host = innerServers.get(i).getHost();
            final int port = innerServers.get(i).getPort();
            final ServerType type = innerServers.get(i).getType();
            new Thread(() -> {
                try {
                    Channel channel = start(host, port, type, id);
                    while (channel == null) {
                        TimeUnit.SECONDS.sleep(5);
                        channel = start(host, port, type, id);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void reconnect(ServerType serverType, Integer serverId) {
        for (int i = 0; i < innerServers.size(); i++) {
            final Integer id = innerServers.get(i).getId();
            if (!Objects.equals(id, serverId)) {
                continue;
            }
            final String host = innerServers.get(i).getHost();
            final int port = innerServers.get(i).getPort();
            final ServerType type = innerServers.get(i).getType();
            new Thread(() -> {
                try {
                    Channel channel = start(host, port, type, id);
                    while (channel == null) {
                        TimeUnit.SECONDS.sleep(3);
                        channel = start(host, port, type, id);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private Channel start(String host, int port, ServerType serverType, Integer id) throws InterruptedException {
        Channel channel = null;
        EventLoopGroup group = new NioEventLoopGroup(1);
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
                                    dispatcher.onMessage(ctx, msg);
                                }
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    });
            channel = b.connect(host, port).sync().channel();
            // 登录消息
            byte[] aPackage = HBSPackage.Builder.withDefaultHeader()
                    .writeInt(HBSMessageType.InnerMessageType.LOGIN)
                    .writeInt(this.serverId)//当前服务器的ID
                    .writeStr(this.serverType.name())//当前服务器的类型
                    .writeInt(id)//登录服务器的ID
                    .writeStr(serverType.name())//登录服务器的类型
                    .buildPackage();
            ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
            channel.writeAndFlush(buf).sync();
            channel.closeFuture().sync();
            System.out.println("client close id:"+ id);
        } catch (Exception e) {
            e.printStackTrace();
            if (Objects.nonNull(channel)) {
                channel.close();
            }
        } finally {
            group.shutdownGracefully();
        }
        return channel;
    }


    public void stop() {
        InnerClientSessionManager.innerLogoutAll();
    }


}
