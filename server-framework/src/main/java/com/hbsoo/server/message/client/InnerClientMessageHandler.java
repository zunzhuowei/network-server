package com.hbsoo.server.message.client;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.server.InnerServerMessageDispatcher;
import com.hbsoo.server.message.server.OuterServerMessageDispatcher;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

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

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中【同协议】
     * @param msg 消息, 类型必须为其中一种：tcp:ByteBuf, websocket:WebSocketFrame, udp:DatagramPacket
     */
    default void redirectMessage(ChannelHandlerContext ctx, T msg) {
        try {
            InnerClientMessageDispatcher innerClientMessageDispatcher = SpringBeanFactory.getBean(InnerClientMessageDispatcher.class);
            innerClientMessageDispatcher.onMessage(ctx, msg);
        } finally {
            int i = ReferenceCountUtil.refCnt(msg);
            if (i > 0) {
                ReferenceCountUtil.release(msg);
            }
        }
        //onMessage(ctx, msg);
    }
}
