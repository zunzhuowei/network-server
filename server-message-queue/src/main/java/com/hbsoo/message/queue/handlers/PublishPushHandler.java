package com.hbsoo.message.queue.handlers;

import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;

/**
 * 客户端接收服务端推送的订阅消息；将消息分发给消息处理器
 * 【客户端测】
 * Created by zun.wei on 2024/6/27.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.PUBLISH_PUSH)
public final class PublishPushHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PublishPushHandler.class);
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String objJson = decoder.readStr();
        String reciTopic = decoder.readStr();
        Map<String, QueueMessageHandler> handlerMap = SpringBeanFactory.getBeansOfType(QueueMessageHandler.class);
        handlerMap.forEach((k, v) -> {
            MessageListener messageListener = AnnotationUtils.findAnnotation(v.getClass(), MessageListener.class);
            if (messageListener != null) {
                String topic = messageListener.topic();
                if (reciTopic.equals(topic)) {
                    innerServerThreadPoolScheduler.execute(objJson, () -> v.handle(objJson));
                }
            } else {
                logger.warn("{} 没有 @MessageListener 注解", v.getClass().getName());
            }
        });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readStr();
    }

}
