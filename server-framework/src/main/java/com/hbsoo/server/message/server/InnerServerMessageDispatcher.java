package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
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
 * Created by zun.wei on 2024/6/12.
 */
public final class InnerServerMessageDispatcher extends ServerMessageDispatcher implements CommonDispatcher  {

    private static final Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> innerServerDispatchers = new ConcurrentHashMap<>();

    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory; //必须注入，保证调用SpringBeanFactory.getBeansWithAnnotation时候，容器已经初始化完成

    @PostConstruct
    protected void init() {
        for (Protocol protocol : Protocol.values()) {
            innerServerDispatchers.computeIfAbsent(protocol, k -> new ConcurrentHashMap<>());
        }
        Map<String, Object> innerHandlers = SpringBeanFactory.getBeansWithAnnotation(InnerServerMessageHandler.class);
        innerHandlers.values().stream()
        .filter(handler -> handler instanceof ServerMessageDispatcher)
        .map(handler -> (ServerMessageDispatcher) handler)
        .forEach(handler -> {
            InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
            Protocol protocol = annotation.protocol();
            String uri = annotation.uri();
            int msgType = annotation.value();
            if (protocol == Protocol.HTTP || !"".equals(uri)) {
                int h;
                msgType = (h = uri.hashCode()) ^ (h >>> 16);
                innerServerDispatchers.get(Protocol.HTTP).putIfAbsent(msgType, handler);
                return;
            }
            innerServerDispatchers.get(protocol).putIfAbsent(msgType, handler);
        });

    }

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) { }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }

    public void onMessage(ChannelHandlerContext ctx, Object msg, Protocol protocol) {
        handleMessage(ctx, msg, protocol);
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, Object msg) {
        handleMessage(ctx, msg);
    }

    @Override
    public Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> dispatchers() {
        return innerServerDispatchers;
    }

    @Override
    public ThreadPoolScheduler threadPoolScheduler() {
        return innerServerThreadPoolScheduler;
    }
}
