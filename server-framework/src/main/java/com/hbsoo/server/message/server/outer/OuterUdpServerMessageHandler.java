package com.hbsoo.server.message.server.outer;

import com.hbsoo.server.message.server.inner.InnerServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class OuterUdpServerMessageHandler implements OuterServerMessageHandler<DatagramPacket> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        final String s = msg.toString();
        System.out.println("UdpMessageHandler = " + s);
        ctx.close();
    }
}
