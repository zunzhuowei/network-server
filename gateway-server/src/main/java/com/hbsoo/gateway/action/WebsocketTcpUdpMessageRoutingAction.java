package com.hbsoo.gateway.action;

import com.hbsoo.gateway.queue.MessageQueueTest;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.DefaultServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class WebsocketTcpUdpMessageRoutingAction extends DefaultServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueTest.class);


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        int msgType = decoder.getMsgType();
        ExtendBody extendBody = decoder.readExtendBody();
        Object threadKey = extendBody.getMsgId();

        NetworkPacket.Builder builder = decoder
                .toBuilder(NetworkPacket.TCP_HEADER)
                .msgType(MessageType.Inside.GATEWAY_ROUTING_MESSAGE_TO_INNER_SERVER)
                .writeExtendBodyMode().writeInt(msgType);
        if (msgType < 1000) {
            forward2InsideServer(builder, "hall", threadKey);
            return;
        }
        if (msgType < 2000) {
            forward2InsideServer(builder, "room", threadKey);
            return;
        }
        if (!(ctx.channel() instanceof NioDatagramChannel)) {
            logger.warn("unknown msgType:{}", msgType);
            ctx.close();
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
