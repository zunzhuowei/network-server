package com.hbsoo.server;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.client.ClientMessageHandler;
import com.hbsoo.server.message.client.TcpClientMessageHandler;
import com.hbsoo.server.session.ServerType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/4.
 */
public final class NetworkClient {

    private TcpClientMessageHandler handlers;
    EventLoopGroup[] groups;
    List<ServerInfo> innerClients;
    public static Map<ServerType, CopyOnWriteArraySet<Channel>> clients = new ConcurrentHashMap<>();

    public NetworkClient(List<ServerInfo> innerClients, TcpClientMessageHandler tcpClientMessageHandler) {
        for (ServerInfo innerClient : innerClients) {
            CopyOnWriteArraySet<Channel> networkClients = clients.get(innerClient.getType());
            if (networkClients == null) {
                networkClients = new CopyOnWriteArraySet<>();
                clients.put(innerClient.getType(), networkClients);
            }
        }
        this.handlers = tcpClientMessageHandler;
        this.innerClients = innerClients;
    }


    public void connect() {
        groups = new NioEventLoopGroup[innerClients.size()];
        for (int i = 0; i < innerClients.size(); i++) {
            final String host = innerClients.get(i).getHost();
            final int port = innerClients.get(i).getPort();
            final ServerType type = innerClients.get(i).getType();
            EventLoopGroup group = groups[i] = new NioEventLoopGroup();
            new Thread(() -> {
                try {
                    start(host, port, type, group);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void start(String host, int port, ServerType serverType, EventLoopGroup group) throws InterruptedException {
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
            Channel channel = b.connect(host, port).sync().channel();
            //ByteBuf buf = Unpooled.copiedBuffer("THBSHello, TCP Server!".getBytes());
            //channel.writeAndFlush(buf).sync();
            clients.get(serverType).add(channel);
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            TimeUnit.SECONDS.sleep(5);
            start(host, port, serverType, group);
        }
    }


    public void stop() {
        for (int i = 0; i < groups.length; i++) {
            groups[i].shutdownGracefully();
        }
        clients.forEach((k,v) -> v.forEach(ChannelOutboundInvoker::close));
    }



}
