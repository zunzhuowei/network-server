package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
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
        log.info("info--" + mqttMessage.toString());
        MqttFixedHeader mqttFixedHeader = mqttMessage.fixedHeader();
        Channel channel = ctx.channel();

        switch (mqttFixedHeader.messageType()) {
            case CONNECT:
                MqttMsgBack.connack(channel, mqttMessage);
                break;
            case PUBLISH:
                MqttMsgBack.puback(channel, mqttMessage);
                break;
            case PUBREL:
                MqttMsgBack.pubcomp(channel, mqttMessage);
                break;
            case SUBSCRIBE:
                MqttMsgBack.suback(channel, mqttMessage);
                break;
            case UNSUBSCRIBE:
                MqttMsgBack.unsuback(channel, mqttMessage);
                break;
            case PINGREQ: //客户端发起心跳
                MqttMsgBack.pingresp(channel, mqttMessage);
                break;
            case DISCONNECT://客户端主动断开连接

                break;
            default:
                break;
        }
    }

}
