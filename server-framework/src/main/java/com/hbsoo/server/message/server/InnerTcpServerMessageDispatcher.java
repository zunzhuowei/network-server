package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/6.
 */
public class InnerTcpServerMessageDispatcher extends TcpServerMessageDispatcher {


    @Override
    public boolean isInnerDispatcher() {
        return true;
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {

    }

}
