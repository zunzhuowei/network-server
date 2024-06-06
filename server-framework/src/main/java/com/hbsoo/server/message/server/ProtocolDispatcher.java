package com.hbsoo.server.message.server;

import com.hbsoo.server.netty.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

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
                ctx.pipeline().addLast(new HttpServerCodec());
                ctx.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                ctx.pipeline().addLast(new HttpRequestHandler(httpServerMessageDispatcher));
                ctx.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
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
        // 其他协议先不处理
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
