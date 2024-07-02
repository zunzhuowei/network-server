package com.hbsoo.gateway.queue;

import com.hbsoo.message.queue.TransactionQueueMessageSenderHandler;
import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.entity.CallbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/27.
 */
@MessageListener(topic = "test", serverType = "hall")
public class MessageQueueTest implements TransactionQueueMessageSenderHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueTest.class);


    @Override
    public void handleCallback(CallbackMessage callbackMessage) {
        logger.info("callbackMessage = {}", callbackMessage);
    }

    @Override
    public int consumerSize() {
        return 2;
    }

    @Override
    public boolean handle(Long msgId, String objJson) {
        logger.debug("handle msgId = {},objJson = {}", msgId, objJson);
        return true;
    }

    @Override
    public boolean rollback(Long msgId, String objJson) {
        logger.debug("rollback msgId = {},objJson = {}", msgId, objJson);
        return true;
    }
}
