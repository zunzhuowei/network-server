package com.hbsoo.server.message.entity;

import com.google.gson.Gson;

/**
 * Created by zun.wei on 2024/6/23.
 */
public interface HBSJsonEntity<T extends HBSJsonEntity<T>> extends HBSEntity<T>{

    default void serializable(HBSPackage.Builder builder, T t){
        Gson gson = new Gson();
        builder.writeStr(gson.toJson(t));
    }

    default T deserialize(HBSPackage.Decoder decoder){
        Gson gson = new Gson();
        return (T) gson.fromJson(decoder.readStr(), this.getClass());
    }

}
