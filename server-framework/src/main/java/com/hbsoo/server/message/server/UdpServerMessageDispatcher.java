package com.hbsoo.server.message.server;

import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class UdpServerMessageDispatcher implements ServerMessageHandler<DatagramPacket>{

    @Override
    public void onMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        final String s = msg.toString();
        System.out.println("UdpMessageHandler = " + s);
        ctx.close();
    }

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
