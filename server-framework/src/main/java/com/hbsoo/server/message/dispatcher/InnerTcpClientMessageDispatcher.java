package com.hbsoo.server.message.dispatcher;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerTcpClientMessageHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/5.
 */
public class InnerTcpClientMessageDispatcher extends InnerTcpClientMessageHandler {


    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {

    }


}
