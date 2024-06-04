package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.inner.InnerTcpServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

public final class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private InnerTcpServerMessageHandler tcpMessageHandler;


    public TcpServerHandler(InnerTcpServerMessageHandler tcpMessageHandler) {
        this.tcpMessageHandler = tcpMessageHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        /*byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        String message = new String(received);
        System.out.println("Received TCP message: " + message);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(("Echo: " + message).getBytes()));*/
        if (Objects.nonNull(tcpMessageHandler)) {
            tcpMessageHandler.onMessage(ctx, msg);
        } else {
            final String s = msg.toString();
            System.err.println("TcpServerHandler not config = " + s);
            ctx.close();
        }
    }
}
