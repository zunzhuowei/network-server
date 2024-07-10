package com.hbsoo.message.queue.handlers;

import com.hbsoo.message.queue.TransactionQueueMessageHandler;
import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;

/**
 * 客户端接收服务端推送的订阅消息；将消息分发给消息处理器
 * 【客户端测】
 * Created by zun.wei on 2024/6/27.
 */
@InsideServerMessageHandler(MessageType.Inside.PUBLISH_PUSH)
public final class PublishPushHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PublishPushHandler.class);
    @Qualifier("insideServerThreadPoolScheduler")
    @Autowired
    private ThreadPoolScheduler threadPoolScheduler;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long msgId = decoder.readLong();
        String objJson = decoder.readStr();
        String reciTopic = decoder.readStr();
        String fromServerType = decoder.readStr();
        int fromServerId = decoder.readInt();
        String mqServerType = decoder.readStr();
        int mqServerId = decoder.readInt();
        Map<String, QueueMessageHandler> handlerMap = SpringBeanFactory.getBeansOfType(QueueMessageHandler.class);
        handlerMap.forEach((k, handler) -> {
            MessageListener messageListener = AnnotationUtils.findAnnotation(handler.getClass(), MessageListener.class);
            if (messageListener != null) {
                String topic = messageListener.topic();
                if (reciTopic.equals(topic)) {
                    threadPoolScheduler.execute(msgId, () -> {
                        boolean handle = handler.handle(msgId, objJson);
                        if (handler instanceof TransactionQueueMessageHandler) {
                            //将执行结果返回消息队列，然后再返回发送者
                            NetworkPacket.Builder builder = decoder.toBuilder()
                                    .writeStr(NowServer.getServerInfo().getType())
                                    .writeInt(NowServer.getServerInfo().getId())
                                    .writeBoolean(handle)
                                    .msgType(MessageType.Inside.RESULT_CALLBACK);
                            InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, mqServerId, mqServerType);
                        }
                    });
                }
            } else {
                logger.warn("PublishPushHandler {} 没有 @MessageListener 注解", handler.getClass().getName());
            }
        });
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readLong();
    }

}
