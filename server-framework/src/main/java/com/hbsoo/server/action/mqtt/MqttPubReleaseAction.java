package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.MqttServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MqttMessageType#PUBREL
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 6, protocol = Protocol.MQTT)
public class MqttPubReleaseAction extends MqttServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        pubcomp(ctx, mqttMessage, extendBody);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    /**
     * 发布完成 qos2
     */
    private void pubcomp(ChannelHandlerContext ctx, MqttMessage mqttMessage, ExtendBody extendBody) {
        MqttMessageIdVariableHeader messageIdVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        //	构建返回报文， 固定报头
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        //	构建返回报文， 可变报头
        MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(messageIdVariableHeader.messageId());
        MqttMessage mqttMessageBack = new MqttMessage(mqttFixedHeaderBack, mqttMessageIdVariableHeaderBack);
        //logger.info("pubcomp--" + mqttMessageBack.toString());
        //channel.writeAndFlush(mqttMessageBack);
        outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, mqttMessageBack, extendBody);
    }
}
