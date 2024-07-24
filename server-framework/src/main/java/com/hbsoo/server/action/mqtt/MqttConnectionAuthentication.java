package com.hbsoo.server.action.mqtt;

import com.hbsoo.server.message.entity.MqttPacket;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/7/24.
 */
public interface MqttConnectionAuthentication {

    /**
     * MQTT 连接认证
     *
     * @param ctx 处理器上下文
     * @param mqttPacket mqtt包
     * @param clientIdentifier 客户端标识符，可以理解是客户端id
     * @param userName 用户名
     * @param password 密码
     * @return 认证结果，true：认证成功，false：认证失败
     */
    boolean authentication(ChannelHandlerContext ctx, MqttPacket mqttPacket, String clientIdentifier, String userName, byte[] password);

}
