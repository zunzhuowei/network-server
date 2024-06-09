package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class UdpServerMessageDispatcher implements ServerMessageHandler<DatagramPacket>{

    protected static final Map<Integer, UdpServerMessageDispatcher> innerUdpServerDispatchers = new ConcurrentHashMap<>();
    protected static final Map<Integer, UdpServerMessageDispatcher> outerUdpServerDispatchers = new ConcurrentHashMap<>();
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;
    @Qualifier("outerServerThreadPoolScheduler")
    @Autowired(required = false)
    private ThreadPoolScheduler outerServerThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory;

    @PostConstruct
    protected void init() {
        final boolean innerDispatcher = isInnerDispatcher();
        final Map<String, Object> handlers = springBeanFactory.getBeansWithAnnotation(innerDispatcher ? InnerServerMessageHandler.class : OuterServerMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            return handler instanceof UdpServerMessageDispatcher;
        }).forEach(handler -> {
            UdpServerMessageDispatcher h = (UdpServerMessageDispatcher) handler;
            if (innerDispatcher) {
                final InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                innerUdpServerDispatchers.put(annotation.value(), h);
            } else {
                final OuterServerMessageHandler annotation = handler.getClass().getAnnotation(OuterServerMessageHandler.class);
                outerUdpServerDispatchers.put(annotation.value(), h);
            }
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            final ByteBuf content = msg.content();
            byte[] received = new byte[content.readableBytes()];
            content.readBytes(received);
            final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
            final int msgType = decoder.readMsgType();
            final boolean innerDispatcher = isInnerDispatcher();
            Map<Integer, UdpServerMessageDispatcher> dispatcherMap = innerDispatcher ? innerUdpServerDispatchers : outerUdpServerDispatchers;
            final UdpServerMessageDispatcher dispatcher = dispatcherMap.get(msgType);
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
            } else {
                System.out.println("没有找到对应的消息处理器");
                ctx.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
        //onMessage(ctx, decoder);
    }

    /**
     * 是否为内部消息
     * @return boolean true 内部消息，false 外部消息
     */
    public abstract boolean isInnerDispatcher();

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
