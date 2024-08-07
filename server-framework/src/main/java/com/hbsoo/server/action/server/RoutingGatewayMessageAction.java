package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.utils.HttpRequestParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 接收网关转发到内网服务器中的http、websocket、tcp、udp消息，
 * 再路由到对应的协议处理器中
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(MessageType.Inside.GATEWAY_ROUTING_MESSAGE_TO_INNER_SERVER)
public class RoutingGatewayMessageAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        //client protocol
        OutsideUserProtocol protocol = extendBody.getOutsideUserProtocol();
        if (protocol == OutsideUserProtocol.HTTP) {
            decoder.resetBodyReadOffset();
            HttpRequestParser parser = HttpRequestParser.parse(decoder);
            redirectAndSwitch2OutsideHttpProtocol(ctx, parser);
            return;
        }
        if (protocol == OutsideUserProtocol.MQTT) {
            decoder.resetBodyReadOffset();
            byte[] bytes = decoder.readBytes();
            try {
                final MqttDecoder mqttDecoder = new MqttDecoder();
                Method decode = mqttDecoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
                decode.setAccessible(true);
                List<MqttMessage> msgs = new ArrayList<>();
                decode.invoke(mqttDecoder, ctx, Unpooled.wrappedBuffer(bytes), msgs);
                redirectAndSwitch2OutsideMqttProtocol(ctx, msgs, extendBody);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            return;
        }
        int msgType = decoder.readExtendBodyMode().readInt();//read msgType from extendBody suffix
        byte[] header = decoder.getHeader();
        decoder.resetBodyReadOffset();
        NetworkPacket.Builder builder = decoder.toBuilder(header).msgType(msgType);
        if (protocol == OutsideUserProtocol.UDP) {
            redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_UDP, builder);
            return;
        }
        if (protocol == OutsideUserProtocol.TCP) {
            redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_TCP, builder);
            return;
        }
        redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_WEBSOCKET, builder);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
