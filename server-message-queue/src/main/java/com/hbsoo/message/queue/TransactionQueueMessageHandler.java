package com.hbsoo.message.queue;

/**
 * 事务接收方端
 * Created by zun.wei on 2024/6/26.
 */
public interface TransactionQueueMessageHandler extends QueueMessageHandler {

    /**
     * 回滚事务
     *
     * @param msgId 消息id
     * @param objJson 消息
     * @return 回滚结果，true：回滚成功，false：回滚失败
     */
    boolean rollback(Long msgId, String objJson);

}
