package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/5/31.
 */
abstract class InnerHttpClientMessageHandler implements InnerClientMessageHandler<FullHttpResponse> {

    protected static final Map<Integer, InnerHttpClientMessageHandler> innerHttpClientDispatchers = new ConcurrentHashMap<>();
    @PostConstruct
    protected void init() {
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            //再判断handler是否为InnerHttpClientMessageHandler的子类
            return handler instanceof InnerHttpClientMessageHandler;
        }).forEach(handler -> {
            final com.hbsoo.server.annotation.InnerClientMessageHandler annotation = handler.getClass().getAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
            InnerHttpClientMessageHandler h = (InnerHttpClientMessageHandler) handler;
            innerHttpClientDispatchers.put(annotation.value(), h);
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpResponse msg) {
        final ByteBuf content = msg.content();
        byte[] received = new byte[content.readableBytes()];
        content.readBytes(received);
        final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
        final int msgType = decoder.readInt();
        final InnerHttpClientMessageHandler dispatcher = innerHttpClientDispatchers.get(msgType);
        if (Objects.nonNull(dispatcher)) {
            dispatcher.onMessage(ctx, decoder);
        }
        //onMessage(ctx, decoder);
    }

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
