package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/12.
 */
public final class OuterServerMessageDispatcher extends ServerMessageDispatcher implements CommonDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(OuterServerMessageDispatcher.class);
    private static final Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> outerServerDispatchers = new ConcurrentHashMap<>();

    @Qualifier("outerServerThreadPoolScheduler")
    @Autowired(required = false)
    private ThreadPoolScheduler outerServerThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory; //必须注入，保证调用SpringBeanFactory.getBeansWithAnnotation时候，容器已经初始化完成

    @PostConstruct
    protected void init() {
        assembleDispatchers(OuterServerMessageHandler.class);
    }

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
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }

    @Override
    public Map<Protocol, ConcurrentHashMap<Integer, ServerMessageDispatcher>> dispatchers() {
        return outerServerDispatchers;
    }

    @Override
    public ThreadPoolScheduler threadPoolScheduler() {
        return outerServerThreadPoolScheduler;
    }
}
