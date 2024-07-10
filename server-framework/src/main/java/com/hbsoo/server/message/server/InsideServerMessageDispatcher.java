package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/12.
 */
public final class InsideServerMessageDispatcher extends ServerMessageDispatcher implements CommonDispatcher  {

    private static final Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> innerServerDispatchers = new ConcurrentHashMap<>();

    @Qualifier("insideServerThreadPoolScheduler")
    @Autowired
    private ThreadPoolScheduler insideServerThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory; //必须注入，保证调用SpringBeanFactory.getBeansWithAnnotation时候，容器已经初始化完成

    @PostConstruct
    protected void init() {
        assembleDispatchers(InsideServerMessageHandler.class);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) { }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
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
        return insideServerThreadPoolScheduler;
    }
}
