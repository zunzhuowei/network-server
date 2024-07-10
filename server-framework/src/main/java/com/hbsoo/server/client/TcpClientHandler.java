package com.hbsoo.server.client;

import com.hbsoo.server.message.client.InsideClientMessageDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final InsideClientMessageDispatcher dispatcher;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TcpClientHandler(InsideClientMessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //byte[] received = new byte[in.readableBytes()];
        //in.readBytes(received);
        //System.out.println("Received: " + new String(received));
        dispatcher.onMessage(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //cause.printStackTrace();
        //ctx.close();
        if (cause instanceof IOException) {
            logger.warn("TcpClientHandler exceptionCaught cause:{}", cause.getMessage());
            return;
        }
        logger.warn("TcpClientHandler exceptionCaught cause:{}", cause);
    }
}