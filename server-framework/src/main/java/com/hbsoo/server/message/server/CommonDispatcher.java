package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.entity.TextWebSocketPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
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

    /**
     * 组装协议分发
     * @param serverMessageHandler 1.InnerServerMessageHandler; OuterServerMessageHandler
     * @param <T> com.hbsoo.server.annotation.InnerServerMessageHandler | com.hbsoo.server.annotation.OuterServerMessageHandler
     */
    default <T extends Annotation> void assembleDispatchers(Class<T> serverMessageHandler) {
        for (Protocol protocol : Protocol.values()) {
            dispatchers().computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        }
        Map<String, Object> innerHandlers = SpringBeanFactory.getBeansWithAnnotation(serverMessageHandler);
        innerHandlers.values().stream()
                .filter(handler -> handler instanceof ServerMessageDispatcher)
                .map(handler -> (ServerMessageDispatcher) handler)
                .forEach(handler -> {
                    T annotation = handler.getClass().getAnnotation(serverMessageHandler);
                    boolean inner = annotation instanceof InnerServerMessageHandler;
                    //boolean outer = annotation instanceof OuterServerMessageHandler;
                    Protocol protocol = inner ? ((InnerServerMessageHandler) annotation).protocol() : ((OuterServerMessageHandler) annotation).protocol();
                    String uri = inner ? ((InnerServerMessageHandler) annotation).uri() : ((OuterServerMessageHandler) annotation).uri();
                    int msgType = inner ? ((InnerServerMessageHandler) annotation).value() : ((OuterServerMessageHandler) annotation).value();
                    if (handler instanceof HttpServerMessageDispatcher) {
                        if (protocol != Protocol.HTTP || "".equals(uri)) {
                            throw new RuntimeException("http message handler must type in protocol and uri !");
                        }
                        int h;
                        msgType = (h = uri.hashCode()) ^ (h >>> 16);
                        dispatchers().get(Protocol.HTTP).putIfAbsent(msgType, handler);
                        return;
                    }
                    if (protocol == Protocol.HTTP) {
                        throw new RuntimeException("http message must extends HttpServerMessageDispatcher!");
                    }
                    dispatchers().get(protocol).putIfAbsent(msgType, handler);
                });
    }

    /**
     * 消息分发
     *
     * @param ctx
     * @param msg
     */
    default void handleMessage(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            handleTcp(ctx, (ByteBuf) msg);
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

    /**
     * 消息分发
     */
    default void handleMessage(ChannelHandlerContext ctx, Object msg, Protocol protocol) {
        if (msg instanceof HBSPackage.Builder) {
            HBSPackage.Builder builder = (HBSPackage.Builder) msg;
            byte[] bytes = builder.buildPackage();
            HBSPackage.Decoder decoder = HBSPackage.Decoder
                    .withHeader(builder.getHeader())
                    .readPackageBody(bytes);
            dispatcher(ctx, protocol, decoder);
            return;
        }
        if (msg instanceof HBSPackage.Decoder) {
            HBSPackage.Decoder decoder = (HBSPackage.Decoder) msg;
            decoder.resetBodyReadOffset();//重置读取偏移
            dispatcher(ctx, protocol, decoder);
            return;
        }
        handleMessage(ctx, msg);
    }

    /**
     * 消息分发
     */
    default void dispatcher(ChannelHandlerContext ctx, Protocol protocol, HBSPackage.Decoder decoder) {
        int msgType = decoder.readMsgType();
        ServerMessageDispatcher dispatcher = dispatchers().get(protocol).get(msgType);
        if (Objects.isNull(dispatcher)) {
            final String s = ctx.channel().id().asShortText();
            logger.warn("消息类型未注册：" + msgType + ",channelID:" + s + ",protocol:" + protocol.name());
            if (protocol == Protocol.UDP) {
                return;
            }
            if (ctx.channel() instanceof NioDatagramChannel) {
                return;
            }
            ctx.close();
            return;
        }
        Object threadKey = dispatcher.threadKey(ctx, decoder);
        decoder.resetBodyReadOffset();//重置读取位置
        decoder.readMsgType();//消息类型
        threadPoolScheduler().execute(threadKey, () -> {
            dispatcher.handle(ctx, decoder);
        });
    }


    /**
     * 处理消息
     */
    default void handleTcp(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            Integer bodyLen = getBodyLen(msg, Protocol.TCP);
            if (bodyLen == null) return;
            byte[] received = new byte[bodyLen + 8];
            msg.readBytes(received);
            HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
            dispatcher(ctx, Protocol.TCP, decoder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理消息
     */
    default void handleUdp(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
        try {
            ByteBuf msg = datagramPacket.content();
            Integer bodyLen = getBodyLen(msg, Protocol.UDP);
            if (bodyLen == null) return;
            byte[] received = new byte[bodyLen + 8];
            msg.readBytes(received);
            HBSPackage.Decoder decoder = HBSPackage.Decoder.withHeader(HBSPackage.UDP_HEADER).readPackageBody(received);
            byte[] bodyData = decoder.readAllTheRestBodyData();
            // 把发送地址包装起来
            HBSPackage.Decoder wrapper = HBSPackage.Builder.withHeader(HBSPackage.UDP_HEADER)
                    .msgType(decoder.getMsgType())
                    .writeStr(datagramPacket.sender().getHostString())
                    .writeInt(datagramPacket.sender().getPort())
                    .writeBytes(bodyData)
                    .toDecoder();
            dispatcher(ctx, Protocol.UDP, wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取消息长度
     *
     * @param msg
     * @param protocol
     * @return
     */
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
                ? Arrays.equals(HBSPackage.TCP_HEADER, headerBytes)
                : Arrays.equals(HBSPackage.UDP_HEADER, headerBytes);
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

    /**
     * 处理Websocket请求
     *
     * @param ctx
     * @param webSocketFrame
     */
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
            Object threadKey = dispatcher.threadKey(ctx, decoder);
            decoder.resetBodyReadOffset();//重置读取位置
            decoder.readMsgType();//消息类型
            threadPoolScheduler().execute(threadKey, () -> {
                dispatcher.handle(ctx, decoder);
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    /**
     * 处理http请求
     *
     * @param ctx
     * @param msg
     */
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
        HttpServerMessageDispatcher dispatcher = (HttpServerMessageDispatcher) dispatchers().get(Protocol.HTTP).get(msgType);
        if (Objects.nonNull(dispatcher)) {
            Object threadKey = dispatcher.threadKey(ctx, null);
            //decoder.resetBodyReadOffset();//重置读取位置
            threadPoolScheduler().execute(threadKey, () -> {
                dispatcher.handle(ctx, httpPackage);
                //ctx.close();
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
