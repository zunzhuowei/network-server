package com.hbsoo.server.message.client;

import com.hbsoo.server.message.entity.HBSPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
interface InnerClientMessageHandler<T> {

    /**
     * 根据返回的值取hash，对线程池取模，指定线程处理消息。
     * 如果返回的值是null，则随机选取线程执行
     */
    Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 处理消息
     */
    void onMessage(ChannelHandlerContext ctx, T msg);


}
