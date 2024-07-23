package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.MqttServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MqttMessageType#PUBCOMP
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 7, protocol = Protocol.MQTT)
public class MqttPubCompAction extends MqttServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
