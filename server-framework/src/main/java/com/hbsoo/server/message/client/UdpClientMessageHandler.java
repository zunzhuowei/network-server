package com.hbsoo.server.message.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class UdpClientMessageHandler implements ClientMessageHandler<DatagramPacket> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        final String s = msg.toString();
        System.out.println("UdpClientMessageHandler = " + s);
        ctx.close();
    }
}
