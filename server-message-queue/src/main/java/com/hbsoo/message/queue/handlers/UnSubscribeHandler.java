package com.hbsoo.message.queue.handlers;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 取消订阅关系
 * 【服务端测】
 * Created by zun.wei on 2024/6/27.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.UN_SUBSCRIBE)
public final class UnSubscribeHandler extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(UnSubscribeHandler.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String serverType = decoder.readStr();
        int serverId = decoder.readInt();
        SubscribeSessionManager.unSubscribe(serverType, serverId);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }

}
