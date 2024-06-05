package com.hbsoo.server.message.server.inner;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerUdpServerMessageHandler implements InnerServerMessageHandler<DatagramPacket> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        final String s = msg.toString();
        System.out.println("UdpMessageHandler = " + s);
        ctx.close();
    }
}
