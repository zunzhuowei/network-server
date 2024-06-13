package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.TextWebSocketPackage;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/13.
 */
interface CommonDispatcher {

    Logger logger = LoggerFactory.getLogger(CommonDispatcher.class);

    //协议分发
    Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> dispatchers();

    //工作线程池
    ThreadPoolScheduler threadPoolScheduler();

    default void handleMessage(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            handle(ctx, (ByteBuf) msg, Protocol.TCP);
            return;
        }
        if (msg instanceof WebSocketFrame) {
            handleWebsocket(ctx, (WebSocketFrame) msg);
            return;
        }
        if (msg instanceof DatagramPacket) {
            handleUdp(ctx, (DatagramPacket) msg);
            return;
        }
        if (msg instanceof FullHttpRequest) {
            handleHttp(ctx, (FullHttpRequest) msg);
            return;
        }
        logger.warn("消息类型未注册：" + msg.getClass().getName());
    }

    default void handle(ChannelHandlerContext ctx, ByteBuf msg, Protocol protocol) {
        try {
            Integer bodyLen = getBodyLen(msg, protocol);
            if (bodyLen == null) return;
            byte[] received = new byte[bodyLen + 8];
            msg.readBytes(received);
            HBSPackage.Decoder decoder = protocol == Protocol.TCP
                    ? HBSPackage.Decoder.withDefaultHeader().readPackageBody(received)
                    : HBSPackage.Decoder.withHeader(new byte[]{'U', 'H', 'B', 'S'}).readPackageBody(received);

            int msgType = decoder.readMsgType();
            ServerMessageDispatcher dispatcher = dispatchers().get(protocol).get(msgType);
            if (Objects.isNull(dispatcher)) {
                final String s = ctx.channel().id().asShortText();
                logger.warn("消息类型未注册：" + msgType + ",channelID:" + s + ",protocol:" + protocol.name());
                received = null;
                ctx.close();
                return;
            }
            threadPoolScheduler().execute(dispatcher.threadKey(decoder), () -> {
                dispatcher.handle(ctx, decoder);
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    default Integer getBodyLen(ByteBuf msg, Protocol protocol) {
        int readableBytes = msg.readableBytes();
        if (readableBytes < 4) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("消息长度小于4：{},{}", readableBytes, new String(received));
            return null;
        }
        byte[] headerBytes = new byte[4];
        msg.getBytes(0, headerBytes);
        boolean matchHeader = protocol == Protocol.TCP
                ? headerBytes[0] == 'T' && headerBytes[1] == 'H' && headerBytes[2] == 'B' && headerBytes[3] == 'S'
                : headerBytes[0] == 'U' && headerBytes[1] == 'H' && headerBytes[2] == 'B' && headerBytes[3] == 'S';
        if (!matchHeader) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("消息头不匹配：{},{}", readableBytes, new String(received));
            return null;
        }
        if (readableBytes < 8) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("消息长度小于8：{},{}", readableBytes, new String(received));
            return null;
        }
        byte[] bodyLenBytes = new byte[4];
        msg.getBytes(4, bodyLenBytes);
        int bodyLen = ByteBuffer.wrap(bodyLenBytes).order(ByteOrder.BIG_ENDIAN).getInt();
        if (readableBytes < 8 + bodyLen) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("包体长度小于{}：{},{}", (8 + bodyLen), readableBytes, new String(received));
            return null;
        }
        return bodyLen;
    }

    default void handleUdp(ChannelHandlerContext ctx, DatagramPacket msg) {
        handle(ctx, msg.content(), Protocol.UDP);
    }

    default void handleWebsocket(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        try {
            final ByteBuf msg = webSocketFrame.content();
            byte[] received = new byte[msg.readableBytes()];
            msg.readBytes(received);
            //TextWebSocketFrame
            boolean isText = webSocketFrame instanceof TextWebSocketFrame;
            if (isText) {
                String jsonStr = new String(received);
                Gson gson = new Gson();
                final TextWebSocketPackage socketPackage = gson.fromJson(jsonStr, TextWebSocketPackage.class);
                final int msgType = socketPackage.getMsgType();
                //把文本消息，转成json格式
                received = HBSPackage.Builder.withDefaultHeader().msgType(msgType).writeStr(jsonStr).buildPackage();
            }
            // BinaryWebSocketFrame
            HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
            int msgType = decoder.readMsgType();
            ServerMessageDispatcher dispatcher = dispatchers().get(Protocol.WEBSOCKET).get(msgType);
            if (Objects.isNull(dispatcher)) {
                try {
                    String _404 = "404";
                    WebSocketFrame response = isText ? new TextWebSocketFrame(_404) : new BinaryWebSocketFrame(Unpooled.wrappedBuffer(_404.getBytes()));
                    ctx.writeAndFlush(response).sync();
                    ctx.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }
            threadPoolScheduler().execute(dispatcher.threadKey(decoder), () -> {
                dispatcher.handle(ctx, decoder);
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    default void handleHttp(ChannelHandlerContext ctx, FullHttpRequest msg) {
        final String path;
        HttpPackage httpPackage = new HttpPackage();
        try {
            final String uri = msg.uri();
            final HttpMethod method = msg.method();
            final HttpHeaders headers = msg.headers();
            final int index = uri.indexOf("?");
            path = index < 0 ? uri : uri.substring(0, index);
            //System.out.println("path:" + path);
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            final Map<String, List<String>> parameters = decoder.parameters();
            final ByteBuf content = msg.content();
            final boolean readable = content.isReadable();
            httpPackage.setHeaders(headers);
            httpPackage.setParameters(parameters);
            httpPackage.setPath(path);
            httpPackage.setUri(uri);
            httpPackage.setFullHttpRequest(msg);
            httpPackage.setMethod(method.name());
            if (readable) {
                byte[] received = new byte[content.readableBytes()];
                content.readBytes(received);
                httpPackage.setBody(received);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ctx.channel().closeFuture().sync();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            return;
        }
        int h;
        int msgType = (h = path.hashCode()) ^ (h >>> 16);
        ServerMessageDispatcher dispatcher = dispatchers().get(Protocol.HTTP).get(msgType);
        if (Objects.nonNull(dispatcher)) {
            threadPoolScheduler().execute(dispatcher.threadKey(null), () -> {
                dispatcher.handle(ctx, httpPackage);
                ctx.close();
            });
        } else {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.NOT_FOUND);
            try {
                ctx.writeAndFlush(response).sync();
                ctx.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
