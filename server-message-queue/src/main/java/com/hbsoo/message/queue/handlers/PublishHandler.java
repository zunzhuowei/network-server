package com.hbsoo.message.queue.handlers;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.InsideClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 接收客户端发布的消息，并转发给客户端
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InsideServerMessageHandler(MessageType.Inside.PUBLISH)
public final class PublishHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PublishHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long msgId = decoder.readLong();
        String objJson = decoder.readStr();
        String topic = decoder.readStr();
        List<ServerInfo> subscribeServers = SubscribeSessionManager.getSubscribeServers(topic, msgId);
        //转发给客户端
        for (ServerInfo subscribeServer : subscribeServers) {
            String type = subscribeServer.getType();
            Integer id = subscribeServer.getId();
            NetworkPacket.Builder builder = decoder.toBuilder()
                    .writeStr(NowServer.getServerInfo().getType())
                    .writeInt(NowServer.getServerInfo().getId())
                    .msgType(MessageType.Inside.PUBLISH_PUSH);
            InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, id, type);
        }
        logger.debug("PublishHandler handle topic:{},objJson:{}", topic, objJson);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readLong();
    }

}
