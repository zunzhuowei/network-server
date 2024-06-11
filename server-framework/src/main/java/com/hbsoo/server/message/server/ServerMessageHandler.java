package com.hbsoo.server.message.server;

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
     * 如果返回的值是null，则随机选取线程执行
     */
    default Object threadKey(HBSPackage.Decoder decoder){
        return null;
    }

    /**
     * 处理消息
     */
    void onMessage(ChannelHandlerContext ctx, T msg);

    /**
     * 消息转发到其他内网服务器的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     *
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param key        键值，用于计算消息应该发送到哪个服务器。
     *                   同时，键值的哈希值被用于决定客户端的选择。
     */
    default void redirectMessage(HBSPackage.Builder msgBuilder, ServerType serverType, Object key) {
        InnerClientSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中
     * @param msg 消息
     */
    default void redirectMessage(ChannelHandlerContext ctx, T msg) {
        //final byte[] buildPackage = msgBuilder.buildPackage();
        //ByteBuf buf = Unpooled.wrappedBuffer(buildPackage);
        onMessage(ctx, msg);
    }

    /**
     * 消息转发到指定协议的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param msgBuilder 消息
     */
    default void redirectMessage(ChannelHandlerContext ctx, ProtocolType protocolType, HBSPackage.Builder msgBuilder) {
        byte[] bytes = msgBuilder.buildPackage();
        ByteBuf msg = Unpooled.wrappedBuffer(bytes);
        try {
            switch (protocolType) {
                case INNER_TCP:
                    InnerTcpServerMessageDispatcher innerTcpServerMessageDispatcher = SpringBeanFactory.getBean("innerTcpServerMessageDispatcher", InnerTcpServerMessageDispatcher.class);
                    innerTcpServerMessageDispatcher.onMessage(ctx, msg);
                    break;
                case INNER_UDP:
                    InnerUdpServerMessageDispatcher innerUdpServerMessageDispatcher = SpringBeanFactory.getBean("innerUdpServerMessageDispatcher", InnerUdpServerMessageDispatcher.class);
                    DatagramPacket msg2 = new DatagramPacket(msg, (InetSocketAddress) ctx.channel().remoteAddress());
                    innerUdpServerMessageDispatcher.onMessage(ctx, msg2);
                    break;
                case INNER_WEBSOCKET:
                    InnerWebsocketServerMessageDispatcher innerWebsocketServerMessageDispatcher = SpringBeanFactory.getBean("innerWebsocketServerMessageDispatcher", InnerWebsocketServerMessageDispatcher.class);
                    WebSocketFrame response = new BinaryWebSocketFrame(msg);
                    innerWebsocketServerMessageDispatcher.onMessage(ctx, response);
                    break;
                case OUTER_TCP:
                    OuterTcpServerMessageDispatcher outerTcpServerMessageDispatcher = SpringBeanFactory.getBean("outerTcpServerMessageDispatcher", OuterTcpServerMessageDispatcher.class);
                    outerTcpServerMessageDispatcher.onMessage(ctx, msg);
                    break;
                case OUTER_UDP:
                    OuterUdpServerMessageDispatcher outerUdpServerMessageDispatcher = SpringBeanFactory.getBean("outerUdpServerMessageDispatcher", OuterUdpServerMessageDispatcher.class);
                    DatagramPacket msg4 = new DatagramPacket(msg, (InetSocketAddress) ctx.channel().remoteAddress());
                    outerUdpServerMessageDispatcher.onMessage(ctx, msg4);
                    break;
                case OUTER_WEBSOCKET:
                    OuterWebsocketServerMessageDispatcher outerWebsocketServerMessageDispatcher = SpringBeanFactory.getBean("outerWebsocketServerMessageDispatcher", OuterWebsocketServerMessageDispatcher.class);
                    WebSocketFrame response2 = new BinaryWebSocketFrame(msg);
                    outerWebsocketServerMessageDispatcher.onMessage(ctx, response2);
                    break;
                }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
