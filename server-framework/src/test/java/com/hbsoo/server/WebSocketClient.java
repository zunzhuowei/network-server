package com.hbsoo.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.websocketx.*;

import java.net.URI;

public class WebSocketClient {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            URI uri = new URI("ws://localhost:5555/ws");
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            String host = uri.getHost() == null ? "localhost" : uri.getHost();
            int port = uri.getPort();

            if (!"ws".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return;
            }

            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, false, null));

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpClientCodec());
                            p.addLast(new HttpObjectAggregator(8192));
                            p.addLast(handler);
                        }
                    });

            Channel ch = b.connect(host, port).sync().channel();
            handler.handshakeFuture().sync();

            for (int i = 0; i < 5000; i++) {
                ch.writeAndFlush(new TextWebSocketFrame("{\n" +
                        "    \"msgType\":1,\n" +
                        "    \"data\":{\n" +
                        "        \"id\":" + i + ",\n" +
                        "        \"name\":\"zhangsan\",\n" +
                        "        \"token\":\"dsxxdaee23fsa\"\n" +
                        "    }\n" +
                        "}")).sync();
            }
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();
            System.out.println("WebSocket Client connected!");
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (status=" + response.status() +
                            ", content=" + response.content().toString(io.netty.util.CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            System.out.println("WebSocket Client received message: " + textFrame.text());
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
            ch.close();
        }
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }
}