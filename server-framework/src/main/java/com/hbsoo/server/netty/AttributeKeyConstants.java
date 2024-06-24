package com.hbsoo.server.netty;

import io.netty.channel.Channel;
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
    /**
     * 用户权限属性
     */
    AttributeKey<String[]> permissionAttr = AttributeKey.valueOf("permission");

    /**
     * 设置属性
     *
     * @param channel   消息管道
     * @param attrName  属性名称
     * @param attrValue 属性值
     * @param <T>       属性类型
     */
    static <T> void setAttr(Channel channel, String attrName, T attrValue) {
        AttributeKey<T> attributeKey = AttributeKey.valueOf(attrName);
        setAttr(channel, attributeKey, attrValue);
    }

    static <T> void setAttr(Channel channel, AttributeKey<T> attributeKey, T attrValue) {
        channel.attr(attributeKey).set(attrValue);
    }

    /**
     * 获取属性
     *
     * @param channel  消息管道
     * @param attrName 属性名称
     * @param <T>      属性类型
     * @return 属性
     */
    static <T> T getAttr(Channel channel, String attrName) {
        AttributeKey<T> attributeKey = AttributeKey.valueOf(attrName);
        return getAttr(channel, attributeKey);
    }

    static <T> T getAttr(Channel channel, AttributeKey<T> attributeKey) {
        return channel.attr(attributeKey).get();
    }

}
