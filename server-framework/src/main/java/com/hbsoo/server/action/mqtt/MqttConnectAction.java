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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MqttMessageType#CONNECT
 * Created by zun.wei on 2024/7/22.
 */
@OutsideMessageHandler(value = 1, protocol = Protocol.MQTT)
public class MqttConnectAction extends MqttServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MqttConnectAction.class);

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Autowired(required = false)
    private MqttConnectionAuthentication mqttConnectionAuthentication;

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        String clientId = ((MqttConnectPayload) mqttMessage.payload()).clientIdentifier();
        String userName = ((MqttConnectPayload) mqttMessage.payload()).userName();
        byte[] password = ((MqttConnectPayload) mqttMessage.payload()).passwordInBytes();
        //authentication logic
        if (mqttConnectionAuthentication != null) {
            boolean authentication = mqttConnectionAuthentication.authentication(ctx, mqttPacket, clientId, userName, password);
            if (!authentication) {
                //outsideUserSessionManager.loginWithMqttAndSyncAllServer();
                logger.warn("login failed,clientId:{},userName:{},password:{}", clientId, userName, new String(password));
                return;
            }
        }
        connack(ctx, mqttMessage, extendBody);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    /**
     * 确认连接请求
     */
    private void connack(ChannelHandlerContext ctx, MqttMessage mqttMessage, ExtendBody extendBody) {
        MqttConnectMessage mqttConnectMessage = (MqttConnectMessage) mqttMessage;
        MqttFixedHeader mqttFixedHeaderInfo = mqttConnectMessage.fixedHeader();
        MqttConnectVariableHeader mqttConnectVariableHeaderInfo = mqttConnectMessage.variableHeader();
        //	构建返回报文， 可变报头
        MqttConnAckVariableHeader mqttConnAckVariableHeaderBack = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, mqttConnectVariableHeaderInfo.isCleanSession());
        //	构建返回报文， 固定报头
        MqttFixedHeader mqttFixedHeaderBack = new MqttFixedHeader(MqttMessageType.CONNACK, mqttFixedHeaderInfo.isDup(), MqttQoS.AT_MOST_ONCE, mqttFixedHeaderInfo.isRetain(), 0x02);
        //	构建CONNACK消息体
        MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeaderBack, mqttConnAckVariableHeaderBack);
        //logger.info("connack--"+connAck.toString());
        //channel.writeAndFlush(connAck);
        outsideUserSessionManager.sendMqttMsg2UserWithChannelId(ctx, connAck, extendBody);
    }
}
