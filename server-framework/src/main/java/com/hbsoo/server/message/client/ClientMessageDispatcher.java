package com.hbsoo.server.message.client;

import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by zun.wei on 2024/6/13.
 */
public abstract class ClientMessageDispatcher implements InsideClientMessageHandler {

    /**
     * 处理消息，tcp, udp, websocket
     */
    public abstract void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder);

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder) {
        redirectMessageOrg(ctx, builder);
    }

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     * @param decoder 注意重置阅读偏移量
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        redirectMessageOrg(ctx, decoder);
    }
    /**
     * 消息转发到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：不支持http协议类型的处理器调用。
     * @param msg 消息, 类型必须为其中一种：
     *            1.tcp:ByteBuf,
     *            2.websocket:WebSocketFrame,
     *            4.udp:DatagramPacket,
     *            5.HBSPackage.Decoder,（推荐）
     *            6.HBSPackage.Builder（推荐）
     */
    private void redirectMessageOrg(ChannelHandlerContext ctx, Object msg) {
        try {
            boolean outerHandler = this.getClass().isAnnotationPresent(com.hbsoo.server.annotation.InsideClientMessageHandler.class);
            if (outerHandler) {
                com.hbsoo.server.annotation.InsideClientMessageHandler handler = this.getClass().getAnnotation(com.hbsoo.server.annotation.InsideClientMessageHandler.class);
                InsideClientMessageDispatcher innerClientMessageDispatcher = SpringBeanFactory.getBean(InsideClientMessageDispatcher.class);
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
