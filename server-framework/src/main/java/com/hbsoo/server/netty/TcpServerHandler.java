package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ServerMessageHandler handler;
    private final Logger logger = LoggerFactory.getLogger(getClass());


    public TcpServerHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        /*byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        String message = new String(received);
        System.out.println("Received TCP message: " + message);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(("Echo: " + message).getBytes()));*/
        handler.onMessage(ctx, msg);
    }

}
