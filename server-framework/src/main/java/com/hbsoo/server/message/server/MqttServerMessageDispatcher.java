package com.hbsoo.server.message.server;

import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.MqttPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by zun.wei on 2024/6/14.
 */
public abstract class MqttServerMessageDispatcher extends ServerMessageDispatcher {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    /**
     * 处理消息，mqtt
     */
    public abstract void handle(ChannelHandlerContext ctx, MqttPacket mqttPacket);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
    }

    /**
     * 转发消息到内部服务中处理
     * @param ctx
     * @param mqttPacket
     * @param serverType
     * @param msgType
     */
    protected void forwardOutsideMqttMsg2InsideServer(ChannelHandlerContext ctx, MqttPacket mqttPacket, String serverType, int msgType) {
        MqttMessage mqttMessage = mqttPacket.getMqttMessage();
        ExtendBody extendBody = mqttPacket.getExtendBody();
        Class<? extends MqttEncoder> aClass = MqttEncoder.INSTANCE.getClass();
        ByteBuf invoke = null;
        try {
            Method doEncode = aClass.getDeclaredMethod("doEncode", ChannelHandlerContext.class, MqttMessage.class);
            doEncode.setAccessible(true);
            invoke = (ByteBuf)doEncode.invoke(MqttEncoder.INSTANCE, ctx, mqttMessage);
            byte [] data = new byte[invoke.readableBytes()];
            invoke.readBytes(data);
            NetworkPacket.Builder msgBuilder = NetworkPacket.Builder
                    .withDefaultHeader()
                    .msgType(msgType)
                    .writeBytes(data)
                    .writeExtendBodyMode()
                    .writeObj(extendBody);
            forward2InsideServer(msgBuilder, serverType, ctx.channel().id().asLongText());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        } finally {
            int i = ReferenceCountUtil.refCnt(invoke);
            if (i > 0) {
                ReferenceCountUtil.release(invoke);
            }
            int i2 = ReferenceCountUtil.refCnt(mqttPacket);
            if (i2 > 0) {
                ReferenceCountUtil.release(mqttPacket);
            }
        }
    }

}
