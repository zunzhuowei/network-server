package com.hbsoo.room;

import com.hbsoo.message.queue.TransactionQueueMessageHandler;
import com.hbsoo.message.queue.config.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/27.
 */
@MessageListener(topic = "test", serverType = "hall")
public class MessageQueueTest implements TransactionQueueMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueTest.class);

    @Override
    public boolean handle(Long msgId, String objJson) {
        logger.debug("handle msgId = {},objJson = {}", msgId, objJson);
        return false;
    }

    @Override
    public boolean rollback(Long msgId, String objJson) {
        logger.debug("rollback msgId = {},objJson = {}", msgId,objJson);
        return false;
    }
}
