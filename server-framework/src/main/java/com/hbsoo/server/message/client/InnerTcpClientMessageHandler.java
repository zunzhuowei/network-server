package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/5/31.
 */
abstract class InnerTcpClientMessageHandler implements InnerClientMessageHandler<ByteBuf> {

    protected static final Map<Integer, InnerTcpClientMessageHandler> innerTcpClientDispatchers = new ConcurrentHashMap<>();
    @Autowired
    private ThreadPoolScheduler innerClientThreadPoolScheduler;

    @PostConstruct
    protected void init() {
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            //再判断handler是否为InnerTcpClientMessageHandler的子类
            return handler instanceof InnerTcpClientMessageHandler;
        }).forEach(handler -> {
            final com.hbsoo.server.annotation.InnerClientMessageHandler annotation = handler.getClass().getAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
            InnerTcpClientMessageHandler h = (InnerTcpClientMessageHandler) handler;
            innerTcpClientDispatchers.put(annotation.value(), h);
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            byte[] received = new byte[msg.readableBytes()];
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

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);


}
