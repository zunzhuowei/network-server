package com.hbsoo.message.queue;

/**
 * 普通消息队列
 * Created by zun.wei on 2024/6/26.
 */
public interface QueueMessageHandler {

    /**
     * 消息处理
     *
     * @param msgId 消息id
     * @param objJson 消息
     * @return 消息处理结果, true: 消息处理成功, false: 消息处理失败
     */
    boolean handle(Long msgId, String objJson);

}
