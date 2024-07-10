package com.hbsoo.server.netty;

import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

//@Component
//@ChannelHandler.Sharable
public final class ProtocolDispatcher extends SimpleChannelInboundHandler<Object> {

    private final ServerMessageHandler handler;
    private final int maxFrameLength;
    private final Set<String> protocols;
    private static final Logger logger = LoggerFactory.getLogger(ProtocolDispatcher.class);
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public ProtocolDispatcher(ServerMessageHandler handler, int maxFrameLength, Set<String> protocols) {
        this.protocols = protocols;
        this.maxFrameLength = maxFrameLength;
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //每隔10秒检查一次链接是否有数据交流。（3 * 10）秒没有数据交流就断开链接
        if (ctx.channel() instanceof NioDatagramChannel) {
            return;
        }
        ctx.pipeline().addLast("unknownIdleStateHandler", new IdleStateHandler(0, 0, 1));
        ctx.pipeline().addLast("unknownServerHeartbeatHandler", new ServerHeartbeatHandler());
        ctx.pipeline().addLast("unknownChannelInactiveHandler", new ChannelInactiveHandler(ProtocolType.UNKNOWN));
        channels.add(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (!(obj instanceof ByteBuf) && !(obj instanceof DatagramPacket)) {
            logger.error("unknown protocol obj: {}", obj.toString());
            ctx.close();
            return;
        }
        ByteBuf msg = obj instanceof ByteBuf ? (ByteBuf) obj : ((DatagramPacket) obj).content();
        ProtocolType protocolType = determineProtocolType(msg);
        if (protocolType == ProtocolType.UNKNOWN) {
            logger.error("unknown protocol type: {}", msg.toString(StandardCharsets.UTF_8));
            ctx.close();
            return;
        }
        if (protocolType != ProtocolType.UDP) {
            ctx.pipeline().remove("unknownIdleStateHandler");
            ctx.pipeline().remove("unknownServerHeartbeatHandler");
            ctx.pipeline().remove("unknownChannelInactiveHandler");
            ctx.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, 10));
            ctx.pipeline().addLast("serverHeartbeatHandler", new ServerHeartbeatHandler());
        }
        if (!protocols.contains(protocolType.name())) {
            logger.error("protocol was disable, type: {}", protocolType.name());
            ctx.close();
            return;
        }
        switch (protocolType) {
            case TCP: {
                ctx.pipeline().addLast(new LengthFieldBasedFrameDecoder
                        (maxFrameLength, NetworkPacket.TCP_HEADER.length, 4, 0, 0));
                ctx.pipeline().addLast(new TcpServerHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case UDP: {
                ctx.pipeline().addLast(new LengthFieldBasedFrameDecoder
                        (maxFrameLength, NetworkPacket.UDP_HEADER.length, 4, 0, 0));
                ctx.pipeline().addLast(new UdpServerHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            case WEBSOCKET: {
                // 添加HTTP编解码器
                ctx.pipeline().addLast(new HttpServerCodec());
                // 可选的压缩支持
                ctx.pipeline().addLast(new WebSocketServerCompressionHandler());
                // 添加WebSocket协议处理器
                ctx.pipeline().addLast(new WebSocketServerProtocolHandler("/ws", null, true));
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
            case MQTT: {
                ctx.pipeline().remove("idleStateHandler");
                ctx.pipeline().remove("serverHeartbeatHandler");
                ctx.pipeline().addLast(MqttEncoder.INSTANCE);
                ctx.pipeline().addLast(new MqttDecoder());
                ctx.pipeline().addLast(new MqttServerHandler(handler));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
                break;
            }
            default: {
                ctx.pipeline().addLast(new UnknownProtocolHandler());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg.retain());
            }
        }
        ctx.pipeline().addLast(new ChannelInactiveHandler(protocolType));
    }


    /**
     * 确定协议类型
     */
    private ProtocolType determineProtocolType(ByteBuf msg) {
        boolean readable = msg.isReadable();
        if (!readable) {
            return ProtocolType.UNKNOWN;
        }
        int readableBytes = msg.readableBytes();
        if (readableBytes >= NetworkPacket.TCP_HEADER.length) {
            byte[] tcpHeader = new byte[NetworkPacket.TCP_HEADER.length];
            msg.getBytes(0, tcpHeader);
            if (Arrays.equals(tcpHeader, NetworkPacket.TCP_HEADER)) {
                return ProtocolType.TCP;
            }
        }
        if (readableBytes >= NetworkPacket.UDP_HEADER.length) {
            byte[] udpHeader = new byte[NetworkPacket.UDP_HEADER.length];
            msg.getBytes(0, udpHeader);
            if (Arrays.equals(udpHeader, NetworkPacket.UDP_HEADER)) {
                return ProtocolType.UDP;
            }
        }
        if (readableBytes < 4) {
            return ProtocolType.UNKNOWN;
        }
        byte[] tempBytes = new byte[4];
        msg.getBytes(0, tempBytes);
        String tag = new String(tempBytes);
        if (tag.startsWith("GET")) {
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
        try {
            boolean mqttFixedHeader = isMqttFixedHeader(msg);
            if (mqttFixedHeader) {
                return ProtocolType.MQTT;
            }
        } catch (Exception e) {
            return ProtocolType.UNKNOWN;
        }
        return ProtocolType.UNKNOWN;
    }

    enum ProtocolType {
        HTTP,
        WEBSOCKET,
        TCP,
        UDP,
        MQTT,
        UNKNOWN
    }

    /**
     * GET /ws HTTP/1.1
     * Sec-WebSocket-Version: 13
     * Sec-WebSocket-Key: RAo8tOERCwmCqGOkHAOrww==
     * Connection: Upgrade
     * Upgrade: websocket
     * Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
     * Host: 127.0.0.1:5555
     * <p>
     * GET /ws HTTP/1.1
     * host: localhost:5555
     * upgrade: websocket
     * connection: upgrade
     * sec-websocket-key: FQ5/lPFU1JlH7G89GSwmaA==
     * origin: http://localhost:5555
     * sec-websocket-version: 13
     */
    private boolean checkRequestHeaders(String requestHeader) {
        final String raw = requestHeader.toLowerCase();
        //boolean containsSecWebSocketVersion = requestHeader.contains("Sec-WebSocket-Version:");
        //boolean containsSecWebSocketKey = requestHeader.contains("Sec-WebSocket-Key:");
        //boolean containsConnectionUpgrade = requestHeader.contains("Connection: Upgrade");
        //boolean containsUpgradeWebsocket = requestHeader.contains("Upgrade: websocket");
        return raw.contains("connection: upgrade") && raw.contains("upgrade: websocket");
    }

    /**
     * 是否为MQTT协议
     */
    private static boolean isMqttFixedHeader(ByteBuf buffer) {
        try {
            short b1 = buffer.readUnsignedByte();
            MqttMessageType messageType = MqttMessageType.valueOf(b1 >> 4);
            boolean dupFlag = (b1 & 8) == 8;
            int qosLevel = (b1 & 6) >> 1;
            boolean retain = (b1 & 1) != 0;
            switch(messageType) {
                case PUBLISH:
                    if (qosLevel == 3) {
                        throw new DecoderException("Illegal QOS Level in fixed header of PUBLISH message (" + qosLevel + ')');
                    }
                    break;
                case PUBREL:
                case SUBSCRIBE:
                case UNSUBSCRIBE:
                    if (dupFlag) {
                        throw new DecoderException("Illegal BIT 3 in fixed header of " + messageType + " message, must be 0, found 1");
                    }

                    if (qosLevel != 1) {
                        throw new DecoderException("Illegal QOS Level in fixed header of " + messageType + " message, must be 1, found " + qosLevel);
                    }

                    if (retain) {
                        throw new DecoderException("Illegal BIT 0 in fixed header of " + messageType + " message, must be 0, found 1");
                    }
                    break;
                case AUTH:
                case CONNACK:
                case CONNECT:
                case DISCONNECT:
                case PINGREQ:
                case PINGRESP:
                case PUBACK:
                case PUBCOMP:
                case PUBREC:
                case SUBACK:
                case UNSUBACK:
                    if (dupFlag) {
                        throw new DecoderException("Illegal BIT 3 in fixed header of " + messageType + " message, must be 0, found 1");
                    }

                    if (qosLevel != 0) {
                        throw new DecoderException("Illegal BIT 2 or 1 in fixed header of " + messageType + " message, must be 0, found " + qosLevel);
                    }

                    if (retain) {
                        throw new DecoderException("Illegal BIT 0 in fixed header of " + messageType + " message, must be 0, found 1");
                    }
                    break;
                default:
                    throw new DecoderException("Unknown message type, do not know how to validate fixed header");
            }

            int remainingLength = 0;
            int multiplier = 1;
            int loops = 0;

            short digit;
            do {
                digit = buffer.readUnsignedByte();
                remainingLength += (digit & 127) * multiplier;
                multiplier *= 128;
                ++loops;
            } while((digit & 128) != 0 && loops < 4);

            if (loops == 4 && (digit & 128) != 0) {
                throw new DecoderException("remaining length exceeds 4 digits (" + messageType + ')');
            } else {
                return true;
            }
        } finally {
            buffer.resetReaderIndex();
        }
    }

}
