package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.sender.ForwardMessageSender;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import com.hbsoo.server.utils.HttpRequestParser;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }

    /**
     * 处理消息，tcp, udp, websocket
     */
    public abstract void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder);

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     * 内网【TCP】协议
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param key        键值，用于计算消息应该发送到哪个服务器。
     *                   同时，键值的哈希值被用于决定客户端的选择。
     */
    public void forward2InnerServer(NetworkPacket.Builder msgBuilder, String serverType, Object key) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }

    /**
     * 延迟转发到【其他内网服务器】的消息处理器中，
     * @param delaySecond 延迟时间（秒）
     */
    public void forward2InnerServer(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2InnerServer(msgBuilder, serverType, key), delaySecond, TimeUnit.SECONDS);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     * 内网【TCP】协议
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param serverId  服务器id。
     */
    public void forward2InnerServer(NetworkPacket.Builder msgBuilder, int serverId, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndId(msgBuilder, serverId, serverType);
    }

    /**
     * 延迟转发到【其他内网服务器】的消息处理器中，
     *
     * @param delaySecond 延迟时间（秒）
     */
    public void forward2InnerServer(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2InnerServer(msgBuilder, serverId, serverType), delaySecond, TimeUnit.SECONDS);
    }

    public void forward2AllInnerServerByType(NetworkPacket.Builder msgBuilder, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAll(msgBuilder, serverType);
    }

    public void forward2AllInnerServerByType(NetworkPacket.Builder msgBuilder, String serverType, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2AllInnerServerByType(msgBuilder, serverType), delaySecond, TimeUnit.SECONDS);
    }

    public void forward2AllInnerServerByTypeUseSender(NetworkPacket.Builder msgBuilder, String serverType) {
        InsideClientSessionManager.forwardMsg2AllServerByTypeUseSender(msgBuilder, serverType);
    }

    public void forward2AllInnerServerByTypeUseSender(NetworkPacket.Builder msgBuilder, String serverType, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2AllInnerServerByTypeUseSender(msgBuilder, serverType), delaySecond, TimeUnit.SECONDS);
    }

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InnerServer}
     */
    public void forward2InnerServerUseSender(NetworkPacket.Builder msgBuilder, String serverType, Object key) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndKeyUseSender(msgBuilder, serverType, key);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InnerServer}
     */
    public void forward2InnerServerUseSender(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2InnerServerUseSender(msgBuilder, serverType, key), delaySecond, TimeUnit.SECONDS);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InnerServer}
     */
    public void forward2InnerServerUseSender(NetworkPacket.Builder msgBuilder, int serverId, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(msgBuilder, serverId, serverType);
    }

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InnerServer}
     */
    public void forward2InnerServerUseSender(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> forward2InnerServerUseSender(msgBuilder, serverId, serverType), delaySecond, TimeUnit.SECONDS);
    }
    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder) {
        redirectMessageOrg(ctx, builder);
    }

    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, builder), delaySecond, TimeUnit.SECONDS);
    }

    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        redirectMessageOrg(ctx, decoder);
    }
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder, int delaySecond) {
        delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder), delaySecond, TimeUnit.SECONDS);
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
            boolean outerHandler = this.getClass().isAnnotationPresent(OutsideMessageHandler.class);
            if (outerHandler) {
                OutsideMessageHandler handler = this.getClass().getAnnotation(OutsideMessageHandler.class);
                redirectAndSwitchProtocolOrg(ctx, ProtocolType.valueOf("OUTSIDE_" + handler.protocol().name()), msg);
                return;
            }
            boolean innerHandler = this.getClass().isAnnotationPresent(InsideServerMessageHandler.class);
            if (innerHandler) {
                InsideServerMessageHandler handler = this.getClass().getAnnotation(InsideServerMessageHandler.class);
                redirectAndSwitchProtocolOrg(ctx, ProtocolType.valueOf("INSIDE_" + handler.protocol().name()), msg);
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
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, NetworkPacket.Builder msgBuilder) {
        redirectAndSwitchProtocolOrg(ctx, protocolType, msgBuilder);
    }

    /**
     * 消息重定向到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param decoder 消息
     */
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, NetworkPacket.Decoder decoder) {
        redirectAndSwitchProtocolOrg(ctx, protocolType, decoder);
    }

    public void redirectAndSwitch2OuterHttp(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        redirectAndSwitchProtocolOrg(ctx, ProtocolType.OUTSIDE_HTTP, fullHttpRequest);
    }

    public void redirectAndSwitch2OuterHttp(ChannelHandlerContext ctx, HttpRequestParser parser) {
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(parser.getMethod()), parser.getUri());
        httpRequest.content().writeBytes(parser.getBody());
        UserSession userSession = parser.getUserSession();
        Map<String, String> headers = parser.getHeaders();
        for (String key : headers.keySet()) {
            httpRequest.headers().set(key, headers.get(key));
        }
        outsideUserSessionManager.login(userSession.getId(), userSession);
        redirectAndSwitch2OuterHttp(ctx, httpRequest);
    }

    /**
     * 消息重定向到【指定协议】的消息处理器中处理,作用于【当前服务器】
     * @param ctx 上下文
     * @param protocolType 协议类型
     * @param customMsg 消息; 1.HBSPackage.Decoder; 2.HBSPackage.Builder;
     */
    private void redirectAndSwitchProtocolOrg(ChannelHandlerContext ctx, ProtocolType protocolType, Object customMsg) {
        InsideServerMessageDispatcher insideServerMessageDispatcher = SpringBeanFactory.getBean(InsideServerMessageDispatcher.class);
        OutsideServerMessageDispatcher outsideServerMessageDispatcher = SpringBeanFactory.getBean(OutsideServerMessageDispatcher.class);
        try {
            switch (protocolType) {
                case INSIDE_TCP:
                    insideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.TCP);
                    break;
                case INSIDE_WEBSOCKET:
                    insideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.WEBSOCKET);
                    break;
                case OUTSIDE_TCP:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.TCP);
                    break;
                case OUTSIDE_WEBSOCKET:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.WEBSOCKET);
                    break;
                case OUTSIDE_HTTP:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.HTTP);
                    break;
            }
        } finally {
            ReferenceCountUtil.release(customMsg);
        }
    }

    /**
     * 请求服务器，并等待服务器响应返回值；
     * 【注意】:方法会在消息体【尾部追加消息ID】。
     * 服务端接收到消息后，响应结果时【必须使用channel】返回，需要【将消息ID也追加到消息体尾部】。
     * @param builder 消息内容
     * @param waitSeconds 等待相应结果时间秒数
     * @param forwardMsg2ServerFunction 消息发送函数
     * @return 服务器响应的内容或者null(等待返回值超时)
     */
    public NetworkPacket.Decoder request2Server(NetworkPacket.Builder builder, int waitSeconds, Consumer<NetworkPacket.Builder> forwardMsg2ServerFunction) {
        try {
            return InsideClientSessionManager.requestServer(builder, waitSeconds, forwardMsg2ServerFunction);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
