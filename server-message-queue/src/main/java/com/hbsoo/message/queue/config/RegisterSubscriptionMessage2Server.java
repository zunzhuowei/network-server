package com.hbsoo.message.queue.config;

import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.message.queue.entity.SubscribeMessage;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.client.InnerTcpClientConnectListener;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.InnerServerMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;

/**
 * 注册订阅的消息到服务端
 * 【客户端测】
 * Created by zun.wei on 2024/6/26.
 */
public final class RegisterSubscriptionMessage2Server implements InnerTcpClientConnectListener {

    private static final Logger logger = LoggerFactory.getLogger(RegisterSubscriptionMessage2Server.class);

    @Override
    public void onConnectSuccess(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index) {
        if (index != 0) {
            return;
        }
        Map<String, QueueMessageHandler> handlerMap = SpringBeanFactory.getBeansOfType(QueueMessageHandler.class);
        handlerMap.forEach((k, v) -> {
            MessageListener messageListener = AnnotationUtils.findAnnotation(v.getClass(), MessageListener.class);
            if (messageListener != null) {
                String topic = messageListener.topic();
                String serverType = messageListener.serverType();
                // 发送订阅消息给队列服务器
                if (toServerInfo.getType().equals(serverType)) {
                    SubscribeMessage subscribeMessage = new SubscribeMessage();
                    subscribeMessage.setTopic(topic);
                    subscribeMessage.setServerType(fromServerInfo.getType());
                    subscribeMessage.setServerId(fromServerInfo.getId());
                    HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                            .msgType(HBSMessageType.Inner.SUBSCRIBE)
                            .writeObj(subscribeMessage);
                    InnerClientSessionManager.forwardMsg2ServerByTypeAndKeyUseSender(builder, serverType, toServerInfo.getId());
                }
            } else {
                logger.warn("{} 没有 @MessageListener 注解", v.getClass().getName());
            }
        });
    }

    @Override
    public void onConnectFail(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index) {
        if (index != 0) {
            return;
        }
        // 外部服务取消订阅
        /*Map<String, QueueMessageHandler> handlerMap = SpringBeanFactory.getBeansOfType(QueueMessageHandler.class);
        handlerMap.forEach((k, v) -> {
            MessageListener messageListener = AnnotationUtils.findAnnotation(v.getClass(), MessageListener.class);
            if (messageListener != null) {
                String topic = messageListener.topic();
                String serverType = messageListener.serverType();
                // 发送取消订阅消息给队列服务器
                if (toServerInfo.getType().equals(serverType)) {
                    SubscribeMessage subscribeMessage = new SubscribeMessage();
                    subscribeMessage.setTopic(topic);
                    subscribeMessage.setServerType(fromServerInfo.getType());
                    subscribeMessage.setServerId(fromServerInfo.getId());
                    HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                            .msgType(HBSMessageType.Inner.UN_SUBSCRIBE)
                            .writeObj(subscribeMessage);
                    InnerClientSessionManager.forwardMsg2ServerByTypeAndKey(builder, serverType, toServerInfo.getId());
                }
            } else {
                logger.warn("{} 没有 @MessageListener 注解", v.getClass().getName());
            }
        });*/
        // 队列服务器自行移除订阅
        InnerServerMessageDispatcher dispatcher = SpringBeanFactory.getBean(InnerServerMessageDispatcher.class);
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Inner.UN_SUBSCRIBE)
                .writeString(toServerInfo.getType())
                .writeInt(toServerInfo.getId());
        dispatcher.onMessage(null, builder, Protocol.TCP);
    }
}
