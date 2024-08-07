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
 * 接收客户端发布的处理结果消息，并转发给发送客户端
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InsideServerMessageHandler(MessageType.Inside.RESULT_CALLBACK)
public final class ResultCallbackHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ResultCallbackHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long msgId = decoder.readLong();
        String objJson = decoder.readStr();
        String topic = decoder.readStr();
        String fromServerType = decoder.readStr();
        int fromServerId = decoder.readInt();
        String mqServerType = decoder.readStr();
        int mqServerId = decoder.readInt();
        String callbackServerType = decoder.readStr();
        int callbackServerId = decoder.readInt();
        boolean callbackResult = decoder.readBoolean();
        //TODO将结果转发给发送端
        NetworkPacket.Builder builder = decoder.toBuilder()
                //.writeStr(callbackServerType)
                //.writeInt(callbackServerId)
                //.writeBoolean(callbackResult)
                .msgType(MessageType.Inside.RESULT_CALLBACK_PUSH);
        InsideClientSessionManager.forwardMsg2ServerByTypeAndIdUseSender(builder, fromServerId, fromServerType);
        logger.debug("PublishPushCallBackHandler handle topic:{},callbackServerType:{},callbackServerId:{},callbackResult:{},objJson:{}",
                topic, callbackServerType, callbackServerId, callbackResult, objJson);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readLong();
    }

}
