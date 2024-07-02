package com.hbsoo.message.queue.handlers;

import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.message.queue.TransactionQueueMessageSenderHandler;
import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.entity.CallbackMessage;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端接收服务端推送的执行回调消息；将消息分发给消息处理器
 * 【发布端的客户端测】
 * Created by zun.wei on 2024/6/27.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.RESULT_CALLBACK_PUSH)
public final class ResultCallbackPushHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ResultCallbackPushHandler.class);
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long msgId = decoder.readLong();
        String objJson = decoder.readStr();
        String reciTopic = decoder.readStr();
        String fromServerType = decoder.readStr();
        int fromServerId = decoder.readInt();
        String mqServerType = decoder.readStr();
        int mqServerId = decoder.readInt();
        String callbackServerType = decoder.readStr();
        int callbackServerId = decoder.readInt();
        boolean callbackResult = decoder.readBoolean();
        Map<String, QueueMessageHandler> handlerMap = SpringBeanFactory.getBeansOfType(QueueMessageHandler.class);
        handlerMap.forEach((k, handler) -> {
            MessageListener messageListener = AnnotationUtils.findAnnotation(handler.getClass(), MessageListener.class);
            if (messageListener != null) {
                String topic = messageListener.topic();
                if (reciTopic.equals(topic)) {
                    if (handler instanceof TransactionQueueMessageSenderHandler) {
                        innerServerThreadPoolScheduler.execute(msgId, () -> {
                            CallbackMessage callbackMessage = new CallbackMessage(
                                    msgId, reciTopic, objJson,
                                    callbackServerType, callbackServerId,
                                    mqServerType, mqServerId,
                                    callbackResult);
                            TransactionQueueMessageSenderHandler v = (TransactionQueueMessageSenderHandler) handler;
                            //缓存回调消息
                            v.callbackMessages.computeIfAbsent(msgId, kk -> new HashSet<>()).add(callbackMessage);
                            //处理回调消息
                            v.handleCallback(callbackMessage);
                            //判断事务流程是否结束，如果结束则释放回调消息；
                            Set<CallbackMessage> callbackMessageSet = v.callbackMessages.get(msgId);
                            boolean result = callbackMessageSet.size() >= v.consumerSize();
                            if (result) {
                                // 如果存在回调失败的消息，则执行回滚
                                boolean match = callbackMessageSet.stream().anyMatch(e -> !e.getCallbackResult());
                                if (match) {
                                    logger.info("事务存在失败，开始执行回滚，msgId:{}", msgId);
                                    v.executeRollback(msgId);
                                }
                                v.callbackMessages.remove(msgId);
                            }
                        });
                    } else {
                        throw new RuntimeException("PublishPushCallbackPushHandler " + handler.getClass().getName()
                                + " 没有实现 TransactionQueueMessageSenderHandler 接口");
                    }
                }
            } else {
                logger.warn("PublishPushCallbackPushHandler {} 没有 @MessageListener 注解", handler.getClass().getName());
            }
        });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readLong();
    }

}
