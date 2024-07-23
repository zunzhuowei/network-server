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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MqttMessageType#SUBSCRIBE
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 8, protocol = Protocol.MQTT)
public class MqttSubscribeAction extends MqttServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        suback(ctx, mqttMessage, extendBody);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    /**
     * 订阅确认
     */
    private void suback(ChannelHandlerContext ctx, MqttMessage mqttMessage, ExtendBody extendBody) {
        MqttSubscribeMessage mqttSubscribeMessage = (MqttSubscribeMessage) mqttMessage;
        MqttMessageIdVariableHeader messageIdVariableHeader = mqttSubscribeMessage.variableHeader();
        //	构建返回报文， 可变报头
        MqttMessageIdVariableHeader variableHeaderBack = MqttMessageIdVariableHeader.from(messageIdVariableHeader.messageId());
        Set<String> topics = mqttSubscribeMessage.payload().topicSubscriptions().stream()
                .map(MqttTopicSubscription::topicName).collect(Collectors.toSet());
        //logger.info(topics.toString());
        List<Integer> grantedQosLevels = new ArrayList<>(topics.size());
        for (int i = 0; i < topics.size(); i++) {
            MqttTopicSubscription topicSubscription = mqttSubscribeMessage.payload().topicSubscriptions().get(i);
            grantedQosLevels.add(topicSubscription.qualityOfService().value());
            MqttSubscribeSessionManager.subscribe(topicSubscription.topicName(),
                    SubscribeInfo.build(extendBody.getFromServerType(),
                            extendBody.getFromServerId(),
                            extendBody.getUserChannelId()));
        }
        //	构建返回报文	有效负载
        MqttSubAckPayload payloadBack = new MqttSubAckPayload(grantedQosLevels);
        //	构建返回报文	固定报头
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2 + topics.size());
        //	构建返回报文	订阅确认
        MqttSubAckMessage subAck = new MqttSubAckMessage(mqttFixedHeaderBack, variableHeaderBack, payloadBack);
        //logger.info("suback--"+subAck.toString());
        //channel.writeAndFlush(subAck);
        outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, subAck, extendBody);
    }
}
