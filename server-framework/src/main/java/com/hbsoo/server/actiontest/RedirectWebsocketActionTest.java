package com.hbsoo.server.actiontest;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.server.InnerWebsocketServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/11.
 */
@InnerServerMessageHandler(value = HBSMessageType.InnerMessageType.LOGOUT)
public class RedirectWebsocketActionTest extends InnerWebsocketServerMessageDispatcher {

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        System.out.println("decoder = " + decoder);

    }

}
