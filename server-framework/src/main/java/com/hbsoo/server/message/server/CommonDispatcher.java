package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.TextWebSocketPacket;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
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
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
    //byte tcp = 0, udp = 1, binary_websocket = 2,text_websocket = 3, http = 4;

    /**
     * 组装协议分发
     * @param serverMessageHandler 1.InsideServerMessageHandler; OutsideServerMessageHandler
     * @param <T> com.hbsoo.server.annotation.InsideServerMessageHandler | com.hbsoo.server.annotation.OutsideServerMessageHandler
     */
    default <T extends Annotation> void assembleDispatchers(Class<T> serverMessageHandler) {
        for (Protocol protocol : Protocol.values()) {
            dispatchers().computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        }
        Map<String, Object> insideHandlers = SpringBeanFactory.getBeansWithAnnotation(serverMessageHandler);
        insideHandlers.values().stream()
                .filter(handler -> handler instanceof ServerMessageDispatcher)
                .map(handler -> (ServerMessageDispatcher) handler)
                .forEach(handler -> {
                    T annotation = AnnotationUtils.findAnnotation(handler.getClass(), serverMessageHandler);
                    //1.获取目标类上的目标注解（可判断目标类是否存在该注解）
                    //HasPermission annotationInClass = AnnotationUtils.findAnnotation(handler.getBeanType(), serverMessageHandler);
                    //2.获取目标方法上的目标注解（可判断目标方法是否存在该注解）
                    //HasPermission annotationInMethod = AnnotationUtils.findAnnotation(handler.getMethod(), serverMessageHandler);
                    if (annotation == null) {
                        return;
                    }
                    boolean inside = annotation instanceof InsideServerMessageHandler;
                    Protocol protocol = inside ? ((InsideServerMessageHandler) annotation).protocol() : ((OutsideMessageHandler) annotation).protocol();
                    String uri = inside ? ((InsideServerMessageHandler) annotation).uri() : ((OutsideMessageHandler) annotation).uri();
                    int msgType = inside ? ((InsideServerMessageHandler) annotation).value() : ((OutsideMessageHandler) annotation).value();
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
        if (msg instanceof NetworkPacket.Builder) {
            NetworkPacket.Builder builder = (NetworkPacket.Builder) msg;
            byte[] bytes = builder.buildPackage();
            NetworkPacket.Decoder decoder = NetworkPacket.Decoder
                    .withHeader(builder.getHeader())
                    .parsePacket(bytes);
            dispatcher(ctx, protocol, decoder);
            return;
        }
        if (msg instanceof NetworkPacket.Decoder) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) msg;
            decoder.resetBodyReadOffset();//重置读取偏移
            dispatcher(ctx, protocol, decoder);
            return;
        }
        handleMessage(ctx, msg);
    }

    /**
     * 消息分发
     */
    default void dispatcher(ChannelHandlerContext ctx, Protocol protocol, NetworkPacket.Decoder decoder) {
        int msgType = decoder.getMsgType();
        ServerMessageDispatcher dispatcher = dispatchers().get(protocol).get(msgType);
        if (Objects.nonNull(dispatcher)) {
            Object threadKey = dispatcher.threadKey(ctx, decoder);
            decoder.resetBodyReadOffset();//重置读取位置
            threadPoolScheduler().execute(threadKey, () -> {
                dispatcher.handle(ctx, decoder);
            });
            return;
        }
        Map<String, DefaultServerMessageDispatcher> beans = SpringBeanFactory.getBeansOfType(DefaultServerMessageDispatcher.class);
        if (!beans.isEmpty()) {
            for (DefaultServerMessageDispatcher messageDispatcher : beans.values()) {
                Object threadKey = messageDispatcher.threadKey(ctx, decoder);
                decoder.resetBodyReadOffset();//重置读取位置
                threadPoolScheduler().execute(threadKey, () -> {
                    messageDispatcher.handle(ctx, decoder);
                });
            }
            return;
        }
        final String s = ctx.channel().id().asShortText();
        logger.warn("消息类型未注册：" + msgType + ",channelID:" + s + ",protocol:" + protocol.name());
        if (protocol == Protocol.UDP) {
            return;
        }
        if (ctx.channel() instanceof NioDatagramChannel) {
            return;
        }
        Boolean isInsideClient = ctx.channel().attr(AttributeKeyConstants.isInsideClientAttr).get();
        if (isInsideClient == null || !isInsideClient) {
            ctx.close();
        }
    }


    /**
     * 处理消息
     */
    default void handleTcp(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            byte[] received = new byte[msg.readableBytes()];
            msg.readBytes(received);
            NetworkPacket.Decoder decoder = NetworkPacket.Decoder.withDefaultHeader().parsePacket(received);
            NetworkPacket.Builder builder = decoder.toBuilder();
            fillExtendBody(ctx, OutsideUserProtocol.TCP.protocolType, decoder, builder, null, 0);
            dispatcher(ctx, Protocol.TCP, builder.toDecoder());
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
            byte[] received = new byte[msg.readableBytes()];
            msg.readBytes(received);
            NetworkPacket.Decoder decoder = NetworkPacket.Decoder.withHeader(NetworkPacket.UDP_HEADER).parsePacket(received);
            NetworkPacket.Builder builder = decoder.toBuilder();
            fillExtendBody(ctx, OutsideUserProtocol.UDP.protocolType, decoder, builder, datagramPacket.sender().getHostString(), datagramPacket.sender().getPort());
            dispatcher(ctx, Protocol.UDP, builder.toDecoder());
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
        int headerLength = protocol == Protocol.TCP ? NetworkPacket.TCP_HEADER.length : NetworkPacket.UDP_HEADER.length;
        byte[] headerBytes = new byte[headerLength];
        msg.getBytes(0, headerBytes);
        boolean matchHeader = protocol == Protocol.TCP
                ? Arrays.equals(NetworkPacket.TCP_HEADER, headerBytes)
                : Arrays.equals(NetworkPacket.UDP_HEADER, headerBytes);
        if (!matchHeader) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("消息头不匹配：{},{}", readableBytes, new String(received));
            return null;
        }
        if (readableBytes < (headerLength + 4)) {//4个字节是消息长度
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("消息长度小于8：{},{}", readableBytes, new String(received));
            return null;
        }
        byte[] bodyLenBytes = new byte[4];//int4字节是消息长度
        msg.getBytes(headerLength, bodyLenBytes);
        int bodyLen = ByteBuffer.wrap(bodyLenBytes).order(ByteOrder.BIG_ENDIAN).getInt();
        if (readableBytes < (headerLength + 4) + bodyLen) {
            byte[] received = new byte[readableBytes];
            msg.getBytes(0, received);
            logger.debug("包体长度小于{}：{},{}", ((headerLength + 4) + bodyLen), readableBytes, new String(received));
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
            NetworkPacket.Decoder decoder;
            //TextWebSocketFrame
            boolean isText = webSocketFrame instanceof TextWebSocketFrame;
            if (isText) {
                String jsonStr = new String(received);
                Gson gson = new Gson();
                TextWebSocketPacket socketPackage = gson.fromJson(jsonStr, TextWebSocketPacket.class);
                int msgType = socketPackage.getMsgType();
                //把文本消息，转成json格式
                NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader().msgType(msgType).writeStr(jsonStr);
                decoder = builder.toDecoder();
            }
            // BinaryWebSocketFrame
            else {
                decoder = NetworkPacket.Decoder.withDefaultHeader().parsePacket(received);
            }
            NetworkPacket.Builder builder = decoder.toBuilder();
            byte type = webSocketFrame instanceof TextWebSocketFrame ? OutsideUserProtocol.TEXT_WEBSOCKET.protocolType : OutsideUserProtocol.BINARY_WEBSOCKET.protocolType;
            fillExtendBody(ctx, type, decoder, builder, null, 0);
            NetworkPacket.Decoder newDecoder = builder.toDecoder();
            int msgType = newDecoder.getMsgType();
            ServerMessageDispatcher dispatcher = dispatchers().get(Protocol.WEBSOCKET).get(msgType);
            if (Objects.nonNull(dispatcher)) {
                Object threadKey = dispatcher.threadKey(ctx, newDecoder);
                newDecoder.resetBodyReadOffset();//重置读取位置
                threadPoolScheduler().execute(threadKey, () -> {
                    dispatcher.handle(ctx, newDecoder);
                });
                return;
            }
            Map<String, DefaultServerMessageDispatcher> beans = SpringBeanFactory.getBeansOfType(DefaultServerMessageDispatcher.class);
            if (!beans.isEmpty()) {
                for (DefaultServerMessageDispatcher messageDispatcher : beans.values()) {
                    Object threadKey = messageDispatcher.threadKey(ctx, newDecoder);
                    newDecoder.resetBodyReadOffset();//重置读取位置
                    threadPoolScheduler().execute(threadKey, () -> {
                        messageDispatcher.handle(ctx, newDecoder);
                    });
                }
                return;
            }
            try {
                String _404 = "404";
                WebSocketFrame response = isText ? new TextWebSocketFrame(_404) : new BinaryWebSocketFrame(Unpooled.wrappedBuffer(_404.getBytes()));
                ctx.writeAndFlush(response).sync();
                Boolean isInsideClient = ctx.channel().attr(AttributeKeyConstants.isInsideClientAttr).get();
                if (isInsideClient == null || !isInsideClient) {
                    ctx.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
    default void handleHttp(ChannelHandlerContext ctx, FullHttpRequest msg, ExtendBody... extendBody) {
        final String path;
        HttpPacket httpPacket = new HttpPacket();
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
            httpPacket.setHeaders(headers);
            httpPacket.setParameters(parameters);
            httpPacket.setPath(path);
            httpPacket.setUri(uri);
            httpPacket.setFullHttpRequest(msg);
            httpPacket.setMethod(method.name());
            if (readable) {
                byte[] received = new byte[content.readableBytes()];
                content.readBytes(received);
                httpPacket.setBody(received);
            }
            if (extendBody == null || extendBody.length == 0) {
                extendBody = new ExtendBody[1];
                extendBody[0] = new ExtendBody();
                SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
                extendBody[0].setMsgId(snowflakeIdGenerator.generateId());
                extendBody[0].setProtocolType(OutsideUserProtocol.HTTP.protocolType);
                extendBody[0].setFromServerId(NowServer.getServerInfo().getId());
                extendBody[0].setFromServerType(NowServer.getServerInfo().getType());
                extendBody[0].setUserChannelId(ctx.channel().id().asLongText());
            }
            httpPacket.setExtendBody(extendBody[0]);
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
        // 类型匹配上了
        HttpServerMessageDispatcher dispatcher = (HttpServerMessageDispatcher) dispatchers().get(Protocol.HTTP).get(msgType);
        if (Objects.nonNull(dispatcher)) {
            Object threadKey = dispatcher.threadKey(ctx, null);
            threadPoolScheduler().execute(threadKey, () -> {
                dispatcher.handle(ctx, httpPacket);
            });
            return;
        }
        // 如果定义的默认处理器
        Map<String, DefaultHttpServerDispatcher> beans = SpringBeanFactory.getBeansOfType(DefaultHttpServerDispatcher.class);
        if (!beans.isEmpty()) {
            for (DefaultHttpServerDispatcher messageDispatcher : beans.values()) {
                Object threadKey = messageDispatcher.threadKey(ctx, null);
                threadPoolScheduler().execute(threadKey, () -> {
                    messageDispatcher.handle(ctx, httpPacket);
                });
            }
            return;
        }
        //如果是内部转发而来的消息
        ExtendBody extendBody2 = httpPacket.getExtendBody();
        OutsideUserSessionManager sessionManager = SpringBeanFactory.getBean(OutsideUserSessionManager.class);
        sessionManager.httpResponse(
                null,
                "404".getBytes(StandardCharsets.UTF_8),
                "application/json; charset=UTF-8",
                extendBody2,
                HttpResponseStatus.NOT_FOUND
        );
    }

    default void fillExtendBody(ChannelHandlerContext ctx, byte protocolType, NetworkPacket.Decoder decoder,
                                NetworkPacket.Builder builder, String senderHost, int senderPort) {
        Boolean isInsideClient = AttributeKeyConstants.getAttr(ctx.channel(), AttributeKeyConstants.isInsideClientAttr);
        boolean hasExtendBody = decoder.hasExtendBody();
        if (!hasExtendBody && isInsideClient == null) {
            Long userId = AttributeKeyConstants.getAttr(ctx.channel(), AttributeKeyConstants.idAttr);
            SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
            ExtendBody extendBody = new ExtendBody();
            extendBody.setMsgId(snowflakeIdGenerator.generateId());
            extendBody.setProtocolType(protocolType);
            extendBody.setFromServerId(NowServer.getServerInfo().getId());
            extendBody.setFromServerType(NowServer.getServerInfo().getType());
            extendBody.setUserChannelId(ctx.channel().id().asLongText());

            OutsideUserSessionManager bean = SpringBeanFactory.getBean(OutsideUserSessionManager.class);
            if (OutsideUserProtocol.UDP.protocolType == protocolType) {
                userId = bean.getUdpRelativeUserId(senderHost, senderPort);
            }
            boolean isLogin = Objects.nonNull(userId);
            if (isLogin) {
                UserSession userSession = bean.getUserSession(userId);
                if (Objects.nonNull(userSession)) {
                    extendBody.setLogin(true);
                    extendBody.setUserId(userId);
                    extendBody.setUserSession(userSession);
                }
            }
            if (extendBody.getProtocolType() == OutsideUserProtocol.UDP.protocolType) {
                extendBody.setSenderHost(senderHost);
                extendBody.setSenderPort(senderPort);
            }
            extendBody.setRetryTimes(0);
            extendBody.serializable(builder);
        }
    }

}
