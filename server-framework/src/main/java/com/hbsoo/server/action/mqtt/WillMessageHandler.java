package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.message.entity.ExtendBody;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * Created by zun.wei on 2024/7/26.
 */
public interface WillMessageHandler {

    void handle(String willTopic, byte[] willMessageInBytes, MqttProperties willProperties, ExtendBody extendBody);

}
