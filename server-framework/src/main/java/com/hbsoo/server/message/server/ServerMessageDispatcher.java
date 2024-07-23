package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import com.hbsoo.server.utils.HttpRequestParser;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by zun.wei on 2024/6/13.
 */
public abstract class ServerMessageDispatcher implements ServerMessageHandler {

    @Autowired
    protected DelayThreadPoolScheduler delayThreadPoolScheduler;

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
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, String serverType, Object key) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }

    /**
     * 延迟转发到【其他内网服务器】的消息处理器中，
     * @param delaySecond 延迟时间（秒）
     */
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        forward2InsideServer(msgBuilder, serverType, key, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2InsideServer(msgBuilder, serverType, key), delay, unit);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 注意：如果转发的服务器类型属于当前服务器类型，则会转发到兄弟服务器中。
     * 内网【TCP】协议
     * @param msgBuilder 消息
     * @param serverType 服务器类型
     * @param serverId  服务器id。
     */
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, int serverId, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndId(msgBuilder, serverId, serverType);
    }

    /**
     * 延迟转发到【其他内网服务器】的消息处理器中，
     *
     * @param delaySecond 延迟时间（秒）
     */
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delaySecond) {
        forward2InsideServer(msgBuilder, serverId, serverType, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2InsideServer(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2InsideServer(msgBuilder, serverId, serverType), delay, unit);
    }

    public void forward2AllInsideServerByType(NetworkPacket.Builder msgBuilder, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAll(msgBuilder, serverType);
    }

    public void forward2AllInsideServerByType(NetworkPacket.Builder msgBuilder, String serverType, int delaySecond) {
        forward2AllInsideServerByType(msgBuilder, serverType, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2AllInsideServerByType(NetworkPacket.Builder msgBuilder, String serverType, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2AllInsideServerByType(msgBuilder, serverType), delay, unit);
    }

    public void forward2AllInsideServerByTypeUseSender(NetworkPacket.Builder msgBuilder, String serverType) {
        InsideClientSessionManager.forwardMsg2AllServerByTypeUseSender(msgBuilder, serverType);
    }

    public void forward2AllInsideServerByTypeUseSender(NetworkPacket.Builder msgBuilder, String serverType, int delaySecond) {
        forward2AllInsideServerByTypeUseSender(msgBuilder, serverType, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2AllInsideServerByTypeUseSender(NetworkPacket.Builder msgBuilder, String serverType, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2AllInsideServerByTypeUseSender(msgBuilder, serverType), delay, unit);
    }

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InsideServer}
     */
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, String serverType, Object key) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndKeyUseSender(msgBuilder, serverType, key);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InsideServer}
     */
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delaySecond) {
        forward2InsideServerUseSender(msgBuilder, serverType, key, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, String serverType, Object key, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2InsideServerUseSender(msgBuilder, serverType, key), delay, unit);
    }
    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InsideServer}
     */
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, int serverId, String serverType) {
        InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(msgBuilder, serverId, serverType);
    }

    /**
     * 消息转发到【其他内网服务器】的消息处理器中，
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link ServerMessageDispatcher#forward2InsideServer}
     */
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delaySecond) {
        forward2InsideServerUseSender(msgBuilder, serverId, serverType, delaySecond, TimeUnit.SECONDS);
    }
    public void forward2InsideServerUseSender(NetworkPacket.Builder msgBuilder, int serverId, String serverType, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> forward2InsideServerUseSender(msgBuilder, serverId, serverType), delay, unit);
    }
    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder) {
        redirectMessageOrg(ctx, builder);
    }

    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder, int delaySecond) {
        redirectMessage(ctx, builder, delaySecond, TimeUnit.SECONDS);
    }
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Builder builder, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, builder), delay, unit);
    }

    /**
     * 消息重定向到【当前服务器】中的其他消息处理器中，与当前处理器【相同协议】
     * 注意：【不支持http、udp协议类型的处理器调用】。
     */
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        redirectMessageOrg(ctx, decoder);
    }
    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder, int delaySecond) {
        redirectMessage(ctx, decoder, delaySecond, TimeUnit.SECONDS);
    }

    public void redirectMessage(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> redirectMessage(ctx, decoder), delay, unit);
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
            boolean outsideHandler = this.getClass().isAnnotationPresent(OutsideMessageHandler.class);
            if (outsideHandler) {
                OutsideMessageHandler handler = this.getClass().getAnnotation(OutsideMessageHandler.class);
                redirectAndSwitchProtocolOrg(ctx, ProtocolType.valueOf("OUTSIDE_" + handler.protocol().name()), msg);
                return;
            }
            boolean insideHandler = this.getClass().isAnnotationPresent(InsideServerMessageHandler.class);
            if (insideHandler) {
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
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, NetworkPacket.Builder msgBuilder, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> redirectAndSwitchProtocolOrg(ctx, protocolType, msgBuilder), delay, unit);
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
    public void redirectAndSwitchProtocol(ChannelHandlerContext ctx, ProtocolType protocolType, NetworkPacket.Decoder decoder, int delay, TimeUnit unit) {
        delayThreadPoolScheduler.schedule(() -> redirectAndSwitchProtocolOrg(ctx, protocolType, decoder), delay, unit);
    }

    /**
     * 消息重定向到【外部HTTP】消息处理器中处理,作用于【当前服务器】
     * @param ctx 处理器上下文
     * @param parser http请求
     */
    public void redirectAndSwitch2OutsideHttpProtocol(ChannelHandlerContext ctx, HttpRequestParser parser) {
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(parser.getMethod()), parser.getUri());
        httpRequest.content().writeBytes(parser.getBody());
        ExtendBody extendBody = parser.getExtendBody();
        Map<String, String> headers = parser.getHeaders();
        for (String key : headers.keySet()) {
            httpRequest.headers().set(key, headers.get(key));
        }
        OutsideServerMessageDispatcher outsideServerMessageDispatcher = SpringBeanFactory.getBean(OutsideServerMessageDispatcher.class);
        outsideServerMessageDispatcher.onHttpMessage(ctx, httpRequest, extendBody);
    }

    /**
     * 消息重定向到【外部MQTT】消息处理器中处理,作用于【当前服务器】
     * @param ctx 处理器上下文
     */
    public void redirectAndSwitch2OutsideMqttProtocol(ChannelHandlerContext ctx, List<MqttMessage> mqttMessages, ExtendBody extendBody) {
        OutsideServerMessageDispatcher outsideServerMessageDispatcher = SpringBeanFactory.getBean(OutsideServerMessageDispatcher.class);
        for (MqttMessage mqttMessage : mqttMessages) {
            outsideServerMessageDispatcher.onMqttMessage(ctx, mqttMessage, extendBody);
        }
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
                case INSIDE_UDP:
                    insideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.UDP);
                    break;
                case OUTSIDE_TCP:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.TCP);
                    break;
                case OUTSIDE_WEBSOCKET:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.WEBSOCKET);
                    break;
                case OUTSIDE_UDP:
                    outsideServerMessageDispatcher.onMessage(ctx, customMsg, Protocol.UDP);
                    break;
            }
        } finally {
            ReferenceCountUtil.release(customMsg);
        }
    }

    /**
     * 请求服务器，并等待服务器响应返回值；
     * @param builder 消息内容
     * @param timeoutSeconds 等待相应结果时间秒数
     * @param forwardMsg2ServerFunction 消息发送函数
     * @return 服务器响应的内容或者null(等待返回值超时)
     */
    public NetworkPacket.Decoder request2Server(NetworkPacket.Builder builder, int timeoutSeconds, Consumer<NetworkPacket.Builder> forwardMsg2ServerFunction) {
        return request2Server(builder, timeoutSeconds, TimeUnit.SECONDS, forwardMsg2ServerFunction);
    }

    public NetworkPacket.Decoder request2Server(NetworkPacket.Builder builder, int timeout, TimeUnit unit, Consumer<NetworkPacket.Builder> forwardMsg2ServerFunction) {
        try {
            return InsideClientSessionManager.requestServer(builder, timeout, unit, forwardMsg2ServerFunction);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
