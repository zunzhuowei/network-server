package com.hbsoo.server.netty;

import io.netty.util.AttributeKey;

/**
 * Created by zun.wei on 2024/6/17.
 */
public interface AttributeKeyConstants {

    /**
     * 空闲次数
     */
    AttributeKey<Integer> idleTimesKey = AttributeKey.valueOf("Heartbeat");
    /**
     * 用户id或者内部服务器id
     */
    AttributeKey<Long> idAttr = AttributeKey.valueOf("id");
    /**
     * 是否为内部客户端
     */
    AttributeKey<Boolean> isInnerClientAttr = AttributeKey.valueOf("isInnerClient");

}
