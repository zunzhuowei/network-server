package com.hbsoo.server.message.entity;

/**
 * Created by zun.wei on 2024/6/23.
 */
public interface HBSEntity<T extends HBSEntity<T>> {

    void serializable(HBSPackage.Builder builder, T t);

    T deserialize(HBSPackage.Decoder decoder);

}
