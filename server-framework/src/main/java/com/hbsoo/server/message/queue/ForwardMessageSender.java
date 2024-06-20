package com.hbsoo.server.message.queue;

import com.hbsoo.server.message.entity.ForwardMessage;

/**
 * Created by zun.wei on 2024/6/19.
 */
public interface ForwardMessageSender {

    /**
     * 发送消息
     */
    void send(ForwardMessage message);

    /**
     * 从数据库转发
     */
    void forwardFormDb();
}
