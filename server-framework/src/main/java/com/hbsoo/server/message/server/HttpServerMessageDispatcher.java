package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class HttpServerMessageDispatcher implements ServerMessageHandler<FullHttpRequest>{

    protected static final Map<Integer, HttpServerMessageDispatcher> innerHttpServerDispatchers = new ConcurrentHashMap<>();
    protected static final Map<Integer, HttpServerMessageDispatcher> outerHttpServerDispatchers = new ConcurrentHashMap<>();
    @PostConstruct
    protected void init() {
        final boolean innerDispatcher = isInnerDispatcher();
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(innerDispatcher ? InnerServerMessageHandler.class : OuterServerMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            return handler instanceof HttpServerMessageDispatcher;
        }).forEach(handler -> {
            HttpServerMessageDispatcher h = (HttpServerMessageDispatcher) handler;
            if (innerDispatcher) {
                final InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                innerHttpServerDispatchers.put(annotation.value(), h);
            } else {
                final OuterServerMessageHandler annotation = handler.getClass().getAnnotation(OuterServerMessageHandler.class);
                outerHttpServerDispatchers.put(annotation.value(), h);
            }
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpRequest msg) {
        final ByteBuf content = msg.content();
        byte[] received = new byte[content.readableBytes()];
        content.readBytes(received);
        final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
        final int msgType = decoder.readInt();
        final boolean innerDispatcher = isInnerDispatcher();
        Map<Integer, HttpServerMessageDispatcher> dispatcherMap = innerDispatcher ? innerHttpServerDispatchers : outerHttpServerDispatchers;
        final HttpServerMessageDispatcher dispatcher = dispatcherMap.get(msgType);
        if (Objects.nonNull(dispatcher)) {
            dispatcher.onMessage(ctx, decoder);
        }
        //onMessage(ctx, decoder);
    }

    public abstract boolean isInnerDispatcher();


    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
