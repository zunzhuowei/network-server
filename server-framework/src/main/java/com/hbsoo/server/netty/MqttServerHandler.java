package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ServerMessageHandler handler;

    public MqttServerHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }

    /**
     * 从客户端收到新的数据时，这个方法会在收到消息时被调用
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        log.debug("mqtt--" + mqttMessage.toString());
        handler.onMessage(ctx, mqttMessage);
    }

}
