package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class TcpServerMessageDispatcher implements ServerMessageHandler<ByteBuf>{

    protected static final Map<Integer, TcpServerMessageDispatcher> innerTcpServerDispatchers = new ConcurrentHashMap<>();
    protected static final Map<Integer, TcpServerMessageDispatcher> outerTcpServerDispatchers = new ConcurrentHashMap<>();
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;
    @Qualifier("outerServerThreadPoolScheduler")
    @Autowired(required = false)
    private ThreadPoolScheduler outerServerThreadPoolScheduler;

    @PostConstruct
    protected void init() {
        final boolean innerDispatcher = isInnerDispatcher();
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(innerDispatcher ? InnerServerMessageHandler.class : OuterServerMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            //再判断handler是否为TcpServerMessageDispatcher的子类
            return handler instanceof TcpServerMessageDispatcher;
        }).forEach(handler -> {
            TcpServerMessageDispatcher h = (TcpServerMessageDispatcher) handler;
            if (innerDispatcher) {
                final InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                innerTcpServerDispatchers.put(annotation.value(), h);
            } else {
                final OuterServerMessageHandler annotation = handler.getClass().getAnnotation(OuterServerMessageHandler.class);
                outerTcpServerDispatchers.put(annotation.value(), h);
            }
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
        final int msgType = decoder.readInt();
        final boolean innerDispatcher = isInnerDispatcher();
        Map<Integer, TcpServerMessageDispatcher> dispatcherMap = innerDispatcher ? innerTcpServerDispatchers : outerTcpServerDispatchers;
        final TcpServerMessageDispatcher dispatcher = dispatcherMap.get(msgType);
        if (Objects.nonNull(dispatcher)) {
            if (innerDispatcher) {
                innerServerThreadPoolScheduler.execute(dispatcher.threadKey(decoder), () -> {
                    dispatcher.onMessage(ctx, decoder);
                });
            } else {
                outerServerThreadPoolScheduler.execute(dispatcher.threadKey(decoder), () -> {
                    dispatcher.onMessage(ctx, decoder);
                });
            }
        }
        //onMessage(ctx, decoder);
    }

    public abstract boolean isInnerDispatcher();

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
