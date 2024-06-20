package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ForwardMessage;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.queue.ForwardMessageSender;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/13.
 */
public abstract class ServerMessageDispatcher implements ServerMessageHandler {

    @Autowired
    protected DelayThreadPoolScheduler delayThreadPoolScheduler;
    @Autowired
    private ForwardMessageSender forwardMessageSender;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }

    /**
     * 处理消息，tcp, udp, websocket
     */
    public abstract void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     * 内网【TCP】协议
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param key        键值，用于计算消息应该发送到哪个服务器。
     *                   同时，键值的哈希值被用于决定客户端的选择。
     */
    public void forward2InnerServer(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        InnerClientSessionManager.forwardMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }
    public void forward2InnerServer(HBSPackage.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        delayThreadPoolScheduler.schedule(() ->
                InnerClientSessionManager.forwardMsg2ServerByTypeAndKey(msgBuilder, serverType, key),
                delaySecond, TimeUnit.SECONDS
        );
    }
    public void forward2InnerServerUseSender(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        long id = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(id, msgBuilder, -1, -1, serverType, key);
        forwardMessageSender.send(forwardMessage);
    }
    public void forward2InnerServerUseSender(HBSPackage.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        long id = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(id, msgBuilder, delaySecond, serverType, key);
        forwardMessageSender.send(forwardMessage);
    }
    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, HBSPackage.Builder builder) {
        redirectMessageOrg(ctx, builder);
    }

    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        redirectMessageOrg(ctx, decoder);
    }
    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     * @param msg 消息, 类型必须为其中一种：
     *            1.tcp:ByteBuf,
     *            2.websocket:WebSocketFrame,
     *            3.HBSPackage.Decoder,（推荐）
     *            4.HBSPackage.Builder（推荐）
     */
    private void redirectMessageOrg(ChannelHandlerContext ctx, Object msg) {
        //final byte[] buildPackage = msgBuilder.buildPackage();
        //ByteBuf buf = Unpooled.wrappedBuffer(buildPackage);
        try {
            boolean outerHandler = this.getClass().isAnnotationPresent(OuterServerMessageHandler.class);
            if (outerHandler) {
                OuterServerMessageHandler handler = this.getClass().getAnnotation(OuterServerMessageHandler.class);
                redirectAndSwitchProtocolOrg(ctx, ProtocolType.valueOf("OUTER_" + handler.protocol().name()), msg);
                return;
            }
            boolean innerHandler = this.getClass().isAnnotationPresent(InnerServerMessageHandler.class);
            if (innerHandler) {
                InnerServerMessageHandler handler = this.getClass().getAnnotation(InnerServerMessageHandler.class);
                redirectAndSwitchProtocolOrg(ctx, ProtocolType.valueOf("INNER_" + handler.protocol().name()), msg);
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
     * 消息重定向到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param msgBuilder 消息
     */
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, HBSPackage.Builder msgBuilder) {
        redirectAndSwitchProtocolOrg(ctx, protocolType, msgBuilder);
    }

    /**
     * 消息重定向到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param decoder 消息
     */
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, HBSPackage.Decoder decoder) {
        redirectAndSwitchProtocolOrg(ctx, protocolType, decoder);
    }

    /**
     * 消息重定向到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param customMsg 消息; 1.HBSPackage.Decoder; 2.HBSPackage.Builder;
     */
    private void redirectAndSwitchProtocolOrg(ChannelHandlerContext ctx, ProtocolType protocolType, Object customMsg) {
        InnerServerMessageDispatcher innerServerMessageDispatcher = SpringBeanFactory.getBean(InnerServerMessageDispatcher.class);
        OuterServerMessageDispatcher outerServerMessageDispatcher = SpringBeanFactory.getBean(OuterServerMessageDispatcher.class);
        try {
            switch (protocolType) {
                case INNER_TCP:
                    innerServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.TCP);
                    break;
                case INNER_WEBSOCKET:
                    innerServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.WEBSOCKET);
                    break;
                case OUTER_TCP:
                    outerServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.TCP);
                    break;
                case OUTER_WEBSOCKET:
                    outerServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.WEBSOCKET);
                    break;
            }
        } finally {
            ReferenceCountUtil.release(customMsg);
        }
    }
}
