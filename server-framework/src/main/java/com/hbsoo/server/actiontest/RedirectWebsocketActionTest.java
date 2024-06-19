package com.hbsoo.server.actiontest;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/11.
 */
@InnerServerMessageHandler(value = HBSMessageType.InnerMessageType.LOGOUT,
        protocol = Protocol.WEBSOCKET)
public class RedirectWebsocketActionTest extends ServerMessageDispatcher {

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        System.out.println("decoder = " + decoder);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
