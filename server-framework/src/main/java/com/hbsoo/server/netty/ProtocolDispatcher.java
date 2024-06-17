package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.StandardCharsets;

//@Component
//@ChannelHandler.Sharable
public final class ProtocolDispatcher extends SimpleChannelInboundHandler<ByteBuf> {

    private final ServerMessageHandler handler;
    private final int maxFrameLength;

    public ProtocolDispatcher(ServerMessageHandler handler, int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //int magicNumber = msg.getUnsignedByte(msg.readerIndex());
        ProtocolType protocolType = determineProtocolType(msg);
        //每隔10秒检查一次链接是否有数据交流。（3 * 10）秒没有数据交流就断开链接
        ctx.pipeline().addLast(new IdleStateHandler(0, 0, 10));
        ctx.pipeline().addLast(new ServerHeartbeatHandler());
        switch (protocolType) {
            case TCP: {
                ctx.pipeline().addLast(new LengthFieldBasedFrameDecoder
                        (maxFrameLength, 4, 4, 0, 0));
                ctx.pipeline().addLast(new TcpServerHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case UDP: {
                ctx.pipeline().addLast(new LengthFieldBasedFrameDecoder
                        (maxFrameLength, 4, 4, 0, 0));
                ctx.pipeline().addLast(new UdpServerHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case WEBSOCKET:{
                // 添加HTTP编解码器
                ctx.pipeline().addLast(new HttpServerCodec());
                // 可选的压缩支持
                ctx.pipeline().addLast(new WebSocketServerCompressionHandler());
                // 添加WebSocket协议处理器
                ctx.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                // 添加WebSocket帧处理器
                ctx.pipeline().addLast(new WebSocketFrameHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case HTTP: {
                // 添加HTTP编解码器
                ctx.pipeline().addLast(new HttpServerCodec());
                // 添加Gzip压缩处理器, 用于压缩HTTP响应消息
                ctx.pipeline().addLast(new HttpContentCompressor());
                // 添加HTTP消息聚合处理器,
                ctx.pipeline().addLast(new HttpObjectAggregator(maxFrameLength));
                // 添加HTTP请求处理器
                ctx.pipeline().addLast(new HttpRequestHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            default:{
                ctx.pipeline().addLast(new UnknownProtocolHandler());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
            }
        }
        ctx.pipeline().addLast(new ChannelInactiveHandler(protocolType));
    }



    private ProtocolType determineProtocolType(ByteBuf msg) {
        int readableBytes = msg.readableBytes();
        if (readableBytes < 4) {
            return ProtocolType.UNKNOWN;
        }
        byte[] tempBytes = new byte[4];
        msg.getBytes(msg.readerIndex(), tempBytes);
        String tag = new String(tempBytes);
        if (tempBytes[0] == 'T' || tempBytes[0] == 'U') {
            if ("THBS".equals(tag)) {
                return ProtocolType.TCP;
            }
            if ("UHBS".equals(tag)) {
                return ProtocolType.UDP;
            }
        }
        if (tag.startsWith("GET")) {
          /*
        GET /ws HTTP/1.1
        Sec-WebSocket-Version: 13
        Sec-WebSocket-Key: RAo8tOERCwmCqGOkHAOrww==
        Connection: Upgrade
        Upgrade: websocket
        Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
        Host: 127.0.0.1:5555
             */
            if (readableBytes > 7) {
                byte[] firstLine = new byte[7];
                msg.getBytes(0, firstLine);
                String firstLineHeader = new String(firstLine);
                if ("GET /ws".equals(firstLineHeader)) {
                    CharSequence charSequence = msg.getCharSequence(0, readableBytes, StandardCharsets.UTF_8);
                    String raw = charSequence.toString();
                    boolean isWebsocket = checkRequestHeaders(raw);
                    if (isWebsocket) {
                        return ProtocolType.WEBSOCKET;
                    }
                }
            }
            return ProtocolType.HTTP;
        }
        if (tag.startsWith("POST")) {
            return ProtocolType.HTTP;
        }
        if (tag.startsWith("PUT")) {
            return ProtocolType.HTTP;
        }
        // DELETE
        if (tag.startsWith("DELE")) {
            return ProtocolType.HTTP;
        }
        // 其他协议先不处理,PATCH,HEAD,OPTIONS,CONNECT,TRACE,PURGE,LINK,UNLINK,COPY,MOVE,PROPFIND,PROPPATCH,MKCOL,LOCK,
        // UNLOCK,SEARCH,M-SEARCH,NOTIFY,SUBSCRIBE,UNSUBSCRIBE,PATCH,MKCALENDAR,VERSION-CONTROL,REPORT,CHECKIN,CHECKOUT
        return ProtocolType.UNKNOWN;
    }

    enum ProtocolType {
        HTTP,
        WEBSOCKET,
        TCP,
        UDP,
        UNKNOWN
    }

    private boolean checkRequestHeaders(String requestHeader) {
        //boolean containsSecWebSocketVersion = requestHeader.contains("Sec-WebSocket-Version:");
        //boolean containsSecWebSocketKey = requestHeader.contains("Sec-WebSocket-Key:");
        boolean containsConnectionUpgrade = requestHeader.contains("Connection: Upgrade");
        boolean containsUpgradeWebsocket = requestHeader.contains("Upgrade: websocket");
        return containsUpgradeWebsocket && containsConnectionUpgrade;
    }

}
