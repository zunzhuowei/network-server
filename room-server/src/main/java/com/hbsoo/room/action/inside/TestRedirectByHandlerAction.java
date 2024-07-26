package com.hbsoo.room.action.inside;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/7/26.
 */
@InsideServerMessageHandler(value = 1111)
public class TestRedirectByHandlerAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        System.out.println("decoder = " + decoder);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
