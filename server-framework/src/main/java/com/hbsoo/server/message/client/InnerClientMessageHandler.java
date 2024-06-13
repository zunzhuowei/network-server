package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
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
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     */
    default void redirectMessage(HBSPackage.Builder builder, ChannelHandlerContext ctx) {
        redirectMessage(ctx, builder);
    }

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     * @param decoder 注意重置阅读偏移量
     */
    default void redirectMessage(HBSPackage.Decoder decoder, ChannelHandlerContext ctx) {
        redirectMessage(ctx, decoder);
    }
    /**
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     * @param msg 消息, 类型必须为其中一种：
     *            1.tcp:ByteBuf,
     *            2.websocket:WebSocketFrame,
     *            4.udp:DatagramPacket,
     *            5.HBSPackage.Decoder,（推荐，注意重置阅读偏移量）
     *            6.HBSPackage.Builder（推荐）
     */
    default void redirectMessage(ChannelHandlerContext ctx, Object msg) {
        try {
            boolean outerHandler = this.getClass().isAnnotationPresent(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
            if (outerHandler) {
                com.hbsoo.server.annotation.InnerClientMessageHandler handler = this.getClass().getAnnotation(com.hbsoo.server.annotation.InnerClientMessageHandler.class);
                InnerClientMessageDispatcher innerClientMessageDispatcher = SpringBeanFactory.getBean(InnerClientMessageDispatcher.class);
                innerClientMessageDispatcher.onMessage(ctx, msg, handler.protocol());
            }
        } finally {
            int i = ReferenceCountUtil.refCnt(msg);
            if (i > 0) {
                ReferenceCountUtil.release(msg);
            }
        }
        //onMessage(ctx, msg);
    }
}
