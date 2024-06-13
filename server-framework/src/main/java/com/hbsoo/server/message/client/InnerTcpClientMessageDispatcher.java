package com.hbsoo.server.message.client;

import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
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
 * Created by zun.wei on 2024/6/5.
 */
public final class InnerTcpClientMessageDispatcher extends InnerTcpClientMessageHandler {

    Logger logger = LoggerFactory.getLogger(InnerTcpClientMessageDispatcher.class);
    protected static final Map<Integer, InnerTcpClientMessageHandler> innerTcpClientDispatchers = new ConcurrentHashMap<>();

    @Autowired
    private ThreadPoolScheduler innerClientThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory;

    /**
     * 处理器初始化逻辑
     */
    @PostConstruct
    protected void init() {
        final Map<String, Object> handlers = springBeanFactory.getBeansWithAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            //再判断handler是否为InnerTcpClientMessageHandler的子类
            return handler instanceof InnerTcpClientMessageHandler;
        }).forEach(handler -> {
            final com.hbsoo.server.annotation.InnerClientMessageHandler annotation = handler.getClass().getAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
            InnerTcpClientMessageHandler h = (InnerTcpClientMessageHandler) handler;
            innerTcpClientDispatchers.putIfAbsent(annotation.value(), h);
        });
    }

    /**
     * 处理器分发逻辑
     */
    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            Integer bodyLen = getBodyLen(msg, Protocol.TCP);
            if (bodyLen == null) return;
            byte[] received = new byte[bodyLen + 8];
            msg.readBytes(received);
            final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
            final int msgType = decoder.readMsgType();
            final InnerTcpClientMessageHandler dispatcher = innerTcpClientDispatchers.get(msgType);
            if (Objects.nonNull(dispatcher)) {
                innerClientThreadPoolScheduler.execute(dispatcher.threadKey(decoder), () -> {
                    dispatcher.onMessage(ctx, decoder);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
        //onMessage(ctx, decoder);
    }

    private Integer getBodyLen(ByteBuf msg, Protocol protocol) {
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

    /**
     * 留给真正的处理器处理
     */
    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }


}
