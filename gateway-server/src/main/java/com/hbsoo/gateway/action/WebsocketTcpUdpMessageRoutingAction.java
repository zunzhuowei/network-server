package com.hbsoo.gateway.action;

import com.hbsoo.gateway.queue.MessageQueueTest;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.DefaultServerMessageDispatcher;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class WebsocketTcpUdpMessageRoutingAction extends DefaultServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueTest.class);

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        int msgType = decoder.getMsgType();
        Object threadKey = threadKey(ctx, decoder);

        decoder.resetBodyReadOffset();
        Long userId = AttributeKeyConstants.getAttr(ctx.channel(), AttributeKeyConstants.idAttr);
        UserSession userSession;
        if (Objects.nonNull(userId)) {
            userSession = outsideUserSessionManager.getUserSession(userId);
            if (Objects.isNull(userSession)) {
                userSession = new UserSession(ctx.channel().id().asLongText());
            }
        } else {
            userSession = new UserSession(ctx.channel().id().asLongText());
        }

        NetworkPacket.Decoder decoder1 = decoder.toBuilder().insertObj2FirstField(userSession).toDecoder();
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER)
                .writeBytes(decoder1.getHeader())
                .writeBytes(decoder1.readAllTheRestBodyData());
        if (msgType < 1000) {
            forward2InnerServer(builder, "hall", threadKey);
            return;
        }
        if (msgType < 2000) {
            forward2InnerServer(builder, "room", threadKey);
            return;
        }
        if (!(ctx.channel() instanceof NioDatagramChannel)) {
            logger.warn("unknown msgType:{}", msgType);
            ctx.close();
        }
        //TODO要用session发送消息必须先登录
        //outerSessionManager.sendMsg2User(decoder.toBuilder(), decoder.getContentType(), decoder.getContent());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        if (ctx.channel() instanceof NioDatagramChannel) {
            String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
            int sendPort = decoder.skipGetInt(NetworkPacket.DecodeSkip.INT, NetworkPacket.DecodeSkip.STRING);
            return sendHost + ":" + sendPort;
        }
        return ctx.channel().id().asLongText();
    }

}
