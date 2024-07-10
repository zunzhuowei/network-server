package com.hbsoo.message.queue.handlers;

import com.hbsoo.message.queue.entity.SubscribeMessage;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 保存订阅关系
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InsideServerMessageHandler(MessageType.Inside.SUBSCRIBE)
public final class SubscribeHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        SubscribeMessage subscribeMessage = new SubscribeMessage();
        decoder.decode2Obj(subscribeMessage);
        String topic = subscribeMessage.getTopic();
        int serverId = subscribeMessage.getServerId();
        String serverType = subscribeMessage.getServerType();
        SubscribeSessionManager.subscribe(topic, serverType, serverId);
        logger.info("保存订阅关系 订阅主题:{},订阅服务器类型:{},订阅服务器id:{}", topic, serverType, serverId);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        SubscribeMessage subscribeMessage = new SubscribeMessage();
        decoder.decode2Obj(subscribeMessage);
        return subscribeMessage.getTopic();
    }

}
