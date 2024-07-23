package com.hbsoo.gateway.action;

import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.DefaultMqttServerDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class MqttMessageRoutingAction extends DefaultMqttServerDispatcher {

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket) {
        forwardOutsideMqttMsg2InsideServer(ctx, mqttPacket, "hall",
                MessageType.Inside.GATEWAY_ROUTING_MESSAGE_TO_INNER_SERVER);
    }

}
