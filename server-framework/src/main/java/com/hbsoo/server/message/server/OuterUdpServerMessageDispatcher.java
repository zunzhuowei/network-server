package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/6.
 */
public class OuterUdpServerMessageDispatcher extends UdpServerMessageDispatcher {


    @Override
    public boolean isInnerDispatcher() {
        return false;
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
    }

}
