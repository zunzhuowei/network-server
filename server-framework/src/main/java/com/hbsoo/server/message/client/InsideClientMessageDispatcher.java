package com.hbsoo.server.message.client;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.SyncMessage;
import com.hbsoo.server.message.entity.TextWebSocketPackage;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
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
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/12.
 */
public final class InsideClientMessageDispatcher extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideClientMessageDispatcher.class);

    private static final Map<Protocol, ConcurrentHashMap<Integer, ClientMessageDispatcher>> innerClientDispatchers = new ConcurrentHashMap<>();

    @Autowired
    protected DelayThreadPoolScheduler delayThreadPoolScheduler;
    @Qualifier("insideClientThreadPoolScheduler")
    @Autowired
    private ThreadPoolScheduler threadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory; //必须注入，保证调用SpringBeanFactory.getBeansWithAnnotation时候，容器已经初始化完成

    @PostConstruct
    protected void init() {
        for (Protocol protocol : Protocol.values()) {
            innerClientDispatchers.computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        }
        Map<String, Object> innerHandlers = SpringBeanFactory.getBeansWithAnnotation(InsideClientMessageHandler.class);
        innerHandlers.values().stream()
        .filter(handler -> handler instanceof ClientMessageDispatcher)
        .map(handler -> (ClientMessageDispatcher) handler)
        .forEach(handler -> {
            InsideClientMessageHandler annotation = handler.getClass().getAnnotation(InsideClientMessageHandler.class);
            Protocol protocol = annotation.protocol();
            int msgType = annotation.value();
            if (protocol == Protocol.HTTP) {
                throw new RuntimeException("un support http protocol!");
            }
            innerClientDispatchers.get(protocol).putIfAbsent(msgType, handler);
        });

    }

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) { }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
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
        if (msg instanceof NetworkPacket.Builder) {
            NetworkPacket.Builder builder = (NetworkPacket.Builder) msg;
            byte[] bytes = builder.buildPackage();
            NetworkPacket.Decoder decoder = NetworkPacket.Decoder
                    .withHeader(builder.getHeader())
                    .readPackageBody(bytes);
            dispatcher(ctx, protocol, decoder);
            return;
        }
        if (msg instanceof NetworkPacket.Decoder) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) msg;
            dispatcher(ctx, protocol, decoder);
            return;
        }
        onMessage(ctx, msg);
    }

    void dispatcher(ChannelHandlerContext ctx, Protocol protocol, NetworkPacket.Decoder decoder) {
        int msgType = decoder.readMsgType();
        //如果是同步消息,服务端返回的结果交给客户端处理
        SyncMessage syncMessage = InsideClientSessionManager.syncMsgMap.get(decoder.getLastLong());
        if (Objects.nonNull(syncMessage)) {
            syncMessage.setDecoder(decoder);
            syncMessage.getCountDownLatch().countDown();
            return;
        }
        ClientMessageDispatcher dispatcher = innerClientDispatchers.get(protocol).get(msgType);
        if (Objects.isNull(dispatcher)) {
            final String s = ctx.channel().id().asShortText();
            logger.warn("消息类型未注册：" + msgType + ",channelID:" + s + ",protocol:" + protocol.name());
            ctx.close();
            return;
        }
        Object threadKey = dispatcher.threadKey(ctx, decoder);
        decoder.resetBodyReadOffset();//重置读取位置
        decoder.readMsgType();//消息类型
        threadPoolScheduler.execute(threadKey , () -> {
            dispatcher.handle(ctx, decoder);
        });
    }

    void handle(ChannelHandlerContext ctx, ByteBuf msg, Protocol protocol) {
        try {
            Integer bodyLen = getBodyLen(msg, protocol);
            if (bodyLen == null) return;
            int headerLength = protocol == Protocol.TCP ? NetworkPacket.TCP_HEADER.length : NetworkPacket.UDP_HEADER.length;
            byte[] received = new byte[headerLength + 4 + bodyLen];
            msg.readBytes(received);
            NetworkPacket.Decoder decoder = protocol == Protocol.TCP
                    ? NetworkPacket.Decoder.withDefaultHeader().readPackageBody(received)
                    : NetworkPacket.Decoder.withHeader(NetworkPacket.UDP_HEADER).readPackageBody(received);

            dispatcher(ctx, protocol, decoder);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    Integer getBodyLen(ByteBuf msg, Protocol protocol) {
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
                received = NetworkPacket.Builder.withDefaultHeader().msgType(msgType).writeStr(jsonStr).buildPackage();
            }
            // BinaryWebSocketFrame
            NetworkPacket.Decoder decoder = NetworkPacket.Decoder.withDefaultHeader().readPackageBody(received);
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
            Object threadKey = dispatcher.threadKey(ctx, decoder);
            decoder.resetBodyReadOffset();//重置读取位置
            decoder.readMsgType();//消息类型
            threadPoolScheduler.execute(threadKey, () -> {
                dispatcher.handle(ctx, decoder);
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }


}
