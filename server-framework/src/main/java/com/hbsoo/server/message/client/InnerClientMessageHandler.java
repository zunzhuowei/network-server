package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
interface InnerClientMessageHandler<T> {

    /**
     * 根据返回的值取hash，对线程池取模，指定线程处理消息
     */
    Object threadKey(HBSPackage.Decoder decoder);

    void onMessage(ChannelHandlerContext ctx, T msg);

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中
     * @param msg 消息
     */
    default void redirectMessage(ChannelHandlerContext ctx, T msg) {
        onMessage(ctx, msg);
    }

}
