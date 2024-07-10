package com.hbsoo.message.queue.handlers;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.InsideClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收发布端发来的回滚事务消息，并转发给客户端
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InsideServerMessageHandler(MessageType.Inside.TRANSACTION_ROLLBACK)
public final class TransactionRollbackHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionRollbackHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long msgId = decoder.readLong();
        String topic = decoder.readStr();
        String objJson = decoder.readStr();
        String serverType = decoder.readStr();
        int serverId = decoder.readInt();
        //转发给客户端
        NetworkPacket.Builder builder = decoder.toBuilder().msgType(MessageType.Inside.TRANSACTION_ROLLBACK_PUSH);
        InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, serverId, serverType);
        logger.debug("RollbackHandler handle msgId:{}, msgId:{},serverType:{},serverId:{},objJson:{}",
                topic, msgId, serverType, serverId, objJson);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readLong();
    }

}
