package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/6.
 */
public class OuterWebsocketServerMessageDispatcher extends WebsocketServerMessageDispatcher {


    @Override
    public boolean isInnerDispatcher() {
        return false;
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
    }

}
