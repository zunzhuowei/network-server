package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerMessageHandler handler;

    public UdpServerHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        handler.onMessage(ctx, packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        //ctx.close();
        if (cause instanceof IOException) {
            logger.warn("UdpServerHandler exceptionCaught ,cause:{}", cause.getMessage());
            return;
        }
        logger.warn("UdpServerHandler exceptionCaught cause:{}", cause);
    }
}