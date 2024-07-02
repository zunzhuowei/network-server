package com.hbsoo.message.queue.handlers;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 接收客户端发布的消息，并转发给客户端
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.PUBLISH)
public final class PublishHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long msgId = decoder.readLong();
        String objJson = decoder.readStr();
        String topic = decoder.readStr();
        List<ServerInfo> subscribeServers = SubscribeSessionManager.getSubscribeServers(topic, msgId);
        //转发给客户端
        for (ServerInfo subscribeServer : subscribeServers) {
            String type = subscribeServer.getType();
            Integer id = subscribeServer.getId();
            HBSPackage.Builder builder = decoder.toBuilder()
                    .writeStr(NowServer.getServerInfo().getType())
                    .writeInt(NowServer.getServerInfo().getId())
                    .msgType(HBSMessageType.Inner.PUBLISH_PUSH);
            InnerClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, id, type);
        }
        logger.debug("PublishHandler handle topic:{},objJson:{}", topic, objJson);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readLong();
    }

}
