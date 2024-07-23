package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.MqttServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MqttMessageType#PINGREQ
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 12, protocol = Protocol.MQTT)
public class MqttPingReqAction extends MqttServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        pingresp(ctx, mqttMessage, extendBody);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    /**
     * 心跳响应
     */
    private void pingresp(ChannelHandlerContext ctx, MqttMessage mqttMessage, ExtendBody extendBody) {
        //	心跳响应报文	11010000 00000000  固定报文
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage mqttMessageBack = new MqttMessage(fixedHeader);
        //logger.info("pingresp--" + mqttMessageBack.toString());
        //channel.writeAndFlush(mqttMessageBack);
        outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, mqttMessageBack, extendBody);
    }
}
