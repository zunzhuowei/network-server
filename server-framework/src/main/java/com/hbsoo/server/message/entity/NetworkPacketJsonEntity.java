package com.hbsoo.server.message.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by zun.wei on 2024/6/23.
 */
public interface NetworkPacketJsonEntity<T extends NetworkPacketJsonEntity<T>> extends NetworkPacketEntity<T> {

    default void serializable(NetworkPacket.Builder builder, T t){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        builder.writeStr(gson.toJson(t));
    }

    default T deserialize(NetworkPacket.Decoder decoder){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        return (T) gson.fromJson(decoder.readStr(), this.getClass());
    }

}
