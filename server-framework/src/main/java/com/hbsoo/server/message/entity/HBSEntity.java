package com.hbsoo.server.message.entity;

/**
 * Created by zun.wei on 2024/6/23.
 */
public interface HBSEntity<T extends HBSEntity<T>> {

    /**
     * 序列化，将对象序列化到builder中
     * @param builder 消息Builder
     * @param t 序列化的对象
     */
    void serializable(HBSPackage.Builder builder, T t);

    /**
     * 反序列化，将消息解码器反序列化到对象中
     * @param decoder 消息解码器
     * @return 目标对象
     */
    T deserialize(HBSPackage.Decoder decoder);

}
