package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.session.ServerType;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

/**
 * Created by zun.wei on 2024/5/31.
 */
public interface ServerMessageHandler<T> {

    /**
     * 根据返回的值取hash，对线程池取模，指定线程处理消息。
     * 如果返回的值是null，则随机选取线程执行;
     * @param decoder 如果http请求，值是null
     */
    Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 处理消息
     */
    void onMessage(ChannelHandlerContext ctx, T msg);

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     *
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param key        键值，用于计算消息应该发送到哪个服务器。
     *                   同时，键值的哈希值被用于决定客户端的选择。
     */
    default void redirect2InnerServer(HBSPackage.Builder msgBuilder, ServerType serverType, Object key) {
        InnerClientSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }

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
        //final byte[] buildPackage = msgBuilder.buildPackage();
        //ByteBuf buf = Unpooled.wrappedBuffer(buildPackage);
        try {
            boolean outerHandler = this.getClass().isAnnotationPresent(OuterServerMessageHandler.class);
            if (outerHandler) {
                OuterServerMessageHandler handler = this.getClass().getAnnotation(OuterServerMessageHandler.class);
                OuterServerMessageDispatcher outerServerMessageDispatcher = SpringBeanFactory.getBean(OuterServerMessageDispatcher.class);
                outerServerMessageDispatcher.onMessage(ctx, msg, handler.protocol());
                return;
            }
            boolean innerHandler = this.getClass().isAnnotationPresent(InnerServerMessageHandler.class);
            if (innerHandler) {
                InnerServerMessageHandler handler = this.getClass().getAnnotation(InnerServerMessageHandler.class);
                InnerServerMessageDispatcher innerServerMessageDispatcher = SpringBeanFactory.getBean(InnerServerMessageDispatcher.class);
                innerServerMessageDispatcher.onMessage(ctx, msg, handler.protocol());
            }
        } finally {
            int i = ReferenceCountUtil.refCnt(msg);
            if (i > 0) {
                ReferenceCountUtil.release(msg);
            }
        }
        //onMessage(ctx, msg);
    }

    /**
     * 消息转发到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param msgBuilder 消息
     */
    default void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, HBSPackage.Builder msgBuilder) {
        byte[] bytes = msgBuilder.buildPackage();
        ByteBuf msg = Unpooled.wrappedBuffer(bytes);
        InnerServerMessageDispatcher innerServerMessageDispatcher = SpringBeanFactory.getBean(InnerServerMessageDispatcher.class);
        OuterServerMessageDispatcher outerServerMessageDispatcher = SpringBeanFactory.getBean(OuterServerMessageDispatcher.class);
        try {
            switch (protocolType) {
                case INNER_TCP:
                    innerServerMessageDispatcher.onMessage(ctx, msg);
                    break;
                case INNER_UDP:
                    DatagramPacket msg2 = new DatagramPacket(msg, (InetSocketAddress) ctx.channel().remoteAddress());
                    innerServerMessageDispatcher.onMessage(ctx, msg2);
                    break;
                case INNER_WEBSOCKET:
                    WebSocketFrame response = new BinaryWebSocketFrame(msg);
                    innerServerMessageDispatcher.onMessage(ctx, response);
                    break;
                case OUTER_TCP:
                    outerServerMessageDispatcher.onMessage(ctx, msg);
                    break;
                case OUTER_UDP:
                    DatagramPacket msg4 = new DatagramPacket(msg, (InetSocketAddress) ctx.channel().remoteAddress());
                    outerServerMessageDispatcher.onMessage(ctx, msg4);
                    break;
                case OUTER_WEBSOCKET:
                    WebSocketFrame response2 = new BinaryWebSocketFrame(msg);
                    outerServerMessageDispatcher.onMessage(ctx, response2);
                    break;
                }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
