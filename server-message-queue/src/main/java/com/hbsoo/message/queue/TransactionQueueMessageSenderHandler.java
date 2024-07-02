package com.hbsoo.message.queue;

import com.hbsoo.message.queue.entity.CallbackMessage;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.session.InnerClientSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事务发送端
 * Created by zun.wei on 2024/6/26.
 */
public interface TransactionQueueMessageSenderHandler extends TransactionQueueMessageHandler {
    Logger logger = LoggerFactory.getLogger(TransactionQueueMessageSenderHandler.class);

    /**
     * 回调消息缓存
     */
    Map<Long, Set<CallbackMessage>> callbackMessages = new ConcurrentHashMap<>();

    /**
     * 处理回调消息,多个服务器订阅就有多次回调。
     * 【注意：需要等待所有服务器都回调之后才能释放回调消息】。
     *
     * @return true: 释放回调消息, false: 保留回调消息
     * @See TransactionQueueMessageHandler#callbackMessage
     */
    default void handleCallback(CallbackMessage callbackMessage){}

    /**
     * 消费者个数。也就是订阅该消息的服务器个数。
     * 包括当前服务器；当前服务器也会消费消息。
     * 用于判断订阅服务器是否都全部处理完成。
     */
    int consumerSize();

    /**
     * 通知各个服务器回滚事务,
     * 必须等到所有服务器回调消息之后才能执行回滚，
     * 否则后面回调的服务器将无法收到回滚消息。
     * @param msgId 事务消息id
     */
    default void executeRollback(Long msgId) {
        Set<CallbackMessage> callbackMessages = TransactionQueueMessageSenderHandler.callbackMessages.get(msgId);
        if (Objects.isNull(callbackMessages) || callbackMessages.isEmpty()) {
            return;
        }
        //将回滚消息发回消息队列，然后再通知服务器回滚事务
        for (CallbackMessage callbackMessage : callbackMessages) {
            boolean success = callbackMessage.getCallbackResult();
            //如果服务器本身处理就失败了，就不发送回滚消息给它了。
            //因为失败了应该由它的本地事务处理了。
            if (!success) {
                continue;
            }
            String topic = callbackMessage.getTopic();
            int mqServerId = callbackMessage.getMqServerId();
            String mqServerType = callbackMessage.getMqServerType();
            String serverType = callbackMessage.getCallbackServerType();
            int serverId = callbackMessage.getCallbackServerId();
            String objJson = callbackMessage.getObjJson();
            Long msgId1 = callbackMessage.getMsgId();
            logger.info("executeRollback msgId:{}, topic:{}, objJson:{}, serverType:{}, serverId:{}, mqServerId:{}, mqServerType:{}",
                    msgId1, topic, objJson, serverType, serverId, mqServerId, mqServerType);
            HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                    .writeLong(msgId1)
                    .writeStr(topic)
                    .writeStr(objJson)
                    .writeStr(serverType)
                    .writeInt(serverId)
                    .msgType(HBSMessageType.Inner.TRANSACTION_ROLLBACK);
            InnerClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, mqServerId, mqServerType);
        }
    }

}
