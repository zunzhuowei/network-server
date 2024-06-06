package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/6.
 */
public class InnerTcpServerMessageDispatcher extends TcpServerMessageDispatcher {


    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final int msgType = decoder.readInt();
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(InnerServerMessageHandler.class);
        //根据消息类型与注解的value只匹配相应的处理器
        handlers.values().stream()
                .filter(handler -> {
                    if (!(handler instanceof InnerTcpServerMessageDispatcher)) {
                        return false;
                    }
                    final InnerServerMessageHandler messageHandler = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                    return messageHandler.value() == msgType;
                })
                .findFirst().ifPresent(handler -> ((InnerTcpServerMessageDispatcher) handler).onMessage(ctx, decoder));
    }

}
