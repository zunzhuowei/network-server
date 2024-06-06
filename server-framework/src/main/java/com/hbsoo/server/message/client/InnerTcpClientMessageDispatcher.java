package com.hbsoo.server.message.client;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/5.
 */
public class InnerTcpClientMessageDispatcher extends InnerTcpClientMessageHandler {


    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final int msgType = decoder.readInt();
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(InnerClientMessageHandler.class);
        //根据消息类型与注解的value只匹配相应的处理器
        handlers.values().stream().filter(handler -> {
            //再判断handler是否为InnerTcpClientMessageDispatcher的子类
            if (!(handler instanceof InnerTcpClientMessageDispatcher)) {
                return false;
            }
            InnerClientMessageHandler innerClientMessageHandler = handler.getClass().getAnnotation(InnerClientMessageHandler.class);
            return innerClientMessageHandler.value() == msgType;
        }).findFirst().ifPresent(handler -> {
            ((InnerTcpClientMessageDispatcher) handler).onMessage(ctx, decoder);
        });
    }


}
