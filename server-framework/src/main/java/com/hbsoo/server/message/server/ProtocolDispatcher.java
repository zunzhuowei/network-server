package com.hbsoo.server.message.server;

import com.hbsoo.server.netty.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

import java.util.Objects;

//@Component
//@ChannelHandler.Sharable
public final class ProtocolDispatcher extends SimpleChannelInboundHandler<ByteBuf> {

    private HttpServerMessageDispatcher httpServerMessageDispatcher;
    private TcpServerMessageDispatcher tcpServerMessageDispatcher;
    private UdpServerMessageDispatcher udpServerMessageDispatcher;
    private WebsocketServerMessageDispatcher websocketServerMessageDispatcher;

    public ProtocolDispatcher(ServerMessageHandler[] handlers) {
        if (Objects.isNull(handlers)) {
            return;
        }
        for (ServerMessageHandler handler : handlers) {
            if (handler instanceof HttpServerMessageDispatcher) {
                this.httpServerMessageDispatcher = (HttpServerMessageDispatcher) handler;
            }
            if (handler instanceof TcpServerMessageDispatcher) {
                this.tcpServerMessageDispatcher = (TcpServerMessageDispatcher) handler;
            }
            if (handler instanceof UdpServerMessageDispatcher) {
                this.udpServerMessageDispatcher = (UdpServerMessageDispatcher) handler;
            }
            if (handler instanceof WebsocketServerMessageDispatcher) {
                this.websocketServerMessageDispatcher = (WebsocketServerMessageDispatcher) handler;
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //int magicNumber = msg.getUnsignedByte(msg.readerIndex());
        ProtocolType protocolType = determineProtocolType(msg);
        switch (protocolType) {
            case TCP: {
                ctx.pipeline().addLast(new TcpServerHandler(tcpServerMessageDispatcher));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case UDP: {
                ctx.pipeline().addLast(new UdpServerHandler(udpServerMessageDispatcher));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case WEBSOCKET:
            case HTTP: {
                // 添加HTTP编解码器
                ctx.pipeline().addLast(new HttpServerCodec());
                // 添加Gzip压缩处理器, 用于压缩HTTP响应消息
                ctx.pipeline().addLast(new HttpContentCompressor());
                // 添加HTTP消息聚合处理器, 最大消息长度为64KB
                ctx.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                // 添加HTTP请求处理器
                ctx.pipeline().addLast(new HttpRequestHandler(httpServerMessageDispatcher));
                // 可选的压缩支持
                ctx.pipeline().addLast(new WebSocketServerCompressionHandler());
                // 添加WebSocket协议处理器
                ctx.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                // 添加WebSocket帧处理器
                ctx.pipeline().addLast(new WebSocketFrameHandler(websocketServerMessageDispatcher));
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
        if (tempBytes[0] == (byte) 0x81) {
            return ProtocolType.WEBSOCKET;
        }
        if (tag.startsWith("GET")) {
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



}
