package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public interface ServerMessageHandler<T> {

    /**
     * 根据返回的值取hash，对线程池取模，指定线程处理消息
     */
    Object threadKey(HBSPackage.Decoder decoder);

    /**
     * 处理消息
     */
    void onMessage(ChannelHandlerContext ctx, T msg);


}
