package com.hbsoo.server.message.entity;

import io.netty.handler.codec.mqtt.MqttMessage;

/**
 * Created by zun.wei on 2024/7/22.
 */
public final class MqttPacket {

    private ExtendBody extendBody;

    private MqttMessage mqttMessage;


    public ExtendBody getExtendBody() {
        return extendBody;
    }

    public void setExtendBody(ExtendBody extendBody) {
        this.extendBody = extendBody;
    }

    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public void setMqttMessage(MqttMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
    }

}
