package com.hbsoo.gateway.action;

import com.hbsoo.gateway.queue.MessageQueueTest;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.DefaultServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class WebsocketTcpUdpMessageRoutingAction extends DefaultServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueTest.class);

    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        int msgType = decoder.getMsgType();
        Object threadKey = threadKey(ctx, decoder);

        decoder.resetBodyReadOffset();
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Inner.GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER)
                .writeBytes(decoder.getHeader())
                .writeBytes(decoder.readAllTheRestBodyData());
        if (msgType < 1000) {
            //login type
            if (msgType == 100) {
                HBSPackage.Decoder decoder1 = decoder.toBuilder()
                        .writeStr(ctx.channel().id().asLongText()).toDecoder();
                builder = HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.Inner.GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER)
                        .writeBytes(decoder1.getHeader())
                        .writeBytes(decoder1.readAllTheRestBodyData());
            }
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
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        if (ctx.channel() instanceof NioDatagramChannel) {
            String sendHost = decoder.skipGetStr(HBSPackage.DecodeSkip.INT);
            int sendPort = decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.STRING);
            return sendHost + ":" + sendPort;
        }
        return ctx.channel().id().asLongText();
    }

}
