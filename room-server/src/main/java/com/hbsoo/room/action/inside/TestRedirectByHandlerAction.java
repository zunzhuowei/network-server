package com.hbsoo.room.action.inside;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/7/26.
 */
@InsideServerMessageHandler(value = 1111)
public class TestRedirectByHandlerAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestRedirectByHandlerAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        logger.debug("decoder = " + decoder);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
