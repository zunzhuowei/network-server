package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.MqttServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MqttMessageType#PUBLISH
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 3, protocol = Protocol.MQTT)
public class MqttPublishAction extends MqttServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        //客户端发布消息 ,PUBACK报文是对QoS 1等级的PUBLISH报文的响应
        String topic = mqttMessage.variableHeader() instanceof MqttPublishVariableHeader ? ((MqttPublishVariableHeader) mqttMessage.variableHeader()).topicName() : "";
        String sMsg = mqttMessage.payload() == null ? "receive message，payload is null，maybe big message length" :
                new String(ByteBufUtil.getBytes((ByteBuf) mqttMessage.payload()), StandardCharsets.UTF_8);
        puback(ctx, mqttMessage, extendBody);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    /**
     * 根据qos发布确认
     */
    private void puback(ChannelHandlerContext ctx, MqttMessage mqttMessage, ExtendBody extendBody) {
        MqttPublishMessage mqttPublishMessage = (MqttPublishMessage) mqttMessage;
        MqttFixedHeader mqttFixedHeaderInfo = mqttPublishMessage.fixedHeader();
        MqttQoS qos = mqttFixedHeaderInfo.qosLevel();
        switch (qos) {
            //	至多一次
            case AT_MOST_ONCE:
                break;
            //	至少一次
            case AT_LEAST_ONCE:
                //	构建返回报文， 可变报头
                MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack = MqttMessageIdVariableHeader.from(mqttPublishMessage.variableHeader().packetId());
                //	构建返回报文， 固定报头
                MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.PUBACK, mqttFixedHeaderInfo.isDup(), MqttQoS.AT_MOST_ONCE, mqttFixedHeaderInfo.isRetain(), 0x02);
                //	构建PUBACK消息体
                MqttPubAckMessage pubAck = new MqttPubAckMessage(mqttFixedHeaderBack, mqttMessageIdVariableHeaderBack);
                //logger.info("AT_LEAST_ONCE puback--" + pubAck.toString());
                //channel.writeAndFlush(pubAck);
                outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, pubAck, extendBody);
                break;
            //	刚好一次
            case EXACTLY_ONCE:
                //	构建返回报文， 固定报头
                MqttFixedHeader mqttFixedHeaderBack2 = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_LEAST_ONCE, false, 0x02);
                //	构建返回报文， 可变报头
                MqttMessageIdVariableHeader mqttMessageIdVariableHeaderBack2 = MqttMessageIdVariableHeader.from(mqttPublishMessage.variableHeader().packetId());
                MqttMessage mqttMessageBack = new MqttMessage(mqttFixedHeaderBack2, mqttMessageIdVariableHeaderBack2);
                //logger.info("EXACTLY_ONCE puback--" + mqttMessageBack.toString());
                //channel.writeAndFlush(mqttMessageBack);
                outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, mqttMessageBack, extendBody);
                break;
            default:
                break;
        }

        //发送消息给客户端
        byte[] payload = new byte[mqttPublishMessage.payload().readableBytes()];
        mqttPublishMessage.payload().readBytes(payload);
        String topicName = mqttPublishMessage.variableHeader().topicName();
        int packetId = mqttPublishMessage.variableHeader().packetId();
        MqttPublishVariableHeader header = new MqttPublishVariableHeader(topicName, packetId);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, payload.length + 5);
        MqttPublishMessage publishMessage = new MqttPublishMessage(mqttFixedHeader, header, Unpooled.wrappedBuffer(payload));
        List<SubscribeInfo> subscribeInfos = MqttSubscribeSessionManager.getSubscribeServers(topicName);
        subscribeInfos.forEach(subscribeInfo -> {
            ExtendBody extendBody2 = new ExtendBody();
            extendBody2.setFromServerId(subscribeInfo.getFromServerId());
            extendBody2.setFromServerType(subscribeInfo.getFromServerType());
            extendBody2.setUserChannelId(subscribeInfo.getUserChannelId());
            outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, publishMessage, extendBody2);
        });
    }

}
