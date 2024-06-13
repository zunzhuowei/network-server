package com.hbsoo.server.message.client;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.TextWebSocketPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/12.
 */
public final class InnerClientMessageDispatcher extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientMessageDispatcher.class);

    private static final Map<Protocol, ConcurrentHashMap<Integer, ClientMessageDispatcher>> innerClientDispatchers = new ConcurrentHashMap<>();

    @Autowired
    private ThreadPoolScheduler innerClientThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory; //必须注入，保证调用SpringBeanFactory.getBeansWithAnnotation时候，容器已经初始化完成

    @PostConstruct
    protected void init() {
        for (Protocol protocol : Protocol.values()) {
            innerClientDispatchers.computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        }
        Map<String, Object> innerHandlers = SpringBeanFactory.getBeansWithAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
        innerHandlers.values().stream()
        .filter(handler -> handler instanceof ClientMessageDispatcher)
        .map(handler -> (ClientMessageDispatcher) handler)
        .forEach(handler -> {
            com.hbsoo.server.annotation.InnerClientMessageHandler annotation = handler.getClass().getAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
            Protocol protocol = annotation.protocol();
            int msgType = annotation.value();
            if (protocol == Protocol.HTTP) {
                throw new RuntimeException("un support http protocol!");
            }
            innerClientDispatchers.get(protocol).putIfAbsent(msgType, handler);
        });

    }

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            handle(ctx, (ByteBuf) msg, Protocol.TCP);
            return;
        }
        if (msg instanceof WebSocketFrame) {
            handleWebsocket(ctx, (WebSocketFrame) msg);
            return;
        }
        if (msg instanceof DatagramPacket) {
            handle(ctx, ((DatagramPacket) msg).content(), Protocol.UDP);
            return;
        }
        logger.warn("消息类型未注册：" + msg.getClass().getName());
    }

    public void onMessage(ChannelHandlerContext ctx, Object msg, Protocol protocol) {
        handleMessage(ctx, msg, protocol);
    }

    void handleMessage(ChannelHandlerContext ctx, Object msg, Protocol protocol) {
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
            dispatcher(ctx, protocol, decoder);
            return;
        }
        onMessage(ctx, msg);
    }

    void dispatcher(ChannelHandlerContext ctx, Protocol protocol, HBSPackage.Decoder decoder) {
        int msgType = decoder.readMsgType();
        ClientMessageDispatcher dispatcher = innerClientDispatchers.get(protocol).get(msgType);
        if (Objects.isNull(dispatcher)) {
            final String s = ctx.channel().id().asShortText();
            logger.warn("消息类型未注册：" + msgType + ",channelID:" + s + ",protocol:" + protocol.name());
            ctx.close();
            return;
        }
        innerClientThreadPoolScheduler.execute(dispatcher.threadKey(ctx, decoder), () -> {
            dispatcher.handle(ctx, decoder);
        });
    }

    void handle(ChannelHandlerContext ctx, ByteBuf msg, Protocol protocol) {
        try {
            Integer bodyLen = getBodyLen(msg, protocol);
            if (bodyLen == null) return;
            byte[] received = new byte[bodyLen + 8];
            msg.readBytes(received);
            HBSPackage.Decoder decoder = protocol == Protocol.TCP
                    ? HBSPackage.Decoder.withDefaultHeader().readPackageBody(received)
                    : HBSPackage.Decoder.withHeader(new byte[]{'U', 'H', 'B', 'S'}).readPackageBody(received);

            dispatcher(ctx, protocol, decoder);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    Integer getBodyLen(ByteBuf msg, Protocol protocol) {
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

    void handleWebsocket(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
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
            ClientMessageDispatcher dispatcher = innerClientDispatchers.get(Protocol.WEBSOCKET).get(msgType);
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
            innerClientThreadPoolScheduler.execute(dispatcher.threadKey(ctx, decoder), () -> {
                dispatcher.handle(ctx, decoder);
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }


}
