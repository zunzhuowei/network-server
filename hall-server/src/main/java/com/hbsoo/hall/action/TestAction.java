package com.hbsoo.hall.action;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/15.
 */
@InnerServerMessageHandler(100)
public class TestAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String s = decoder.readStr();
        //System.out.println("s = " + s);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return ctx.channel().id().asShortText();
    }
}
