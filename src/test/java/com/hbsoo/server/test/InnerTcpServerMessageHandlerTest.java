package com.hbsoo.server.test;

import com.hbsoo.server.message.server.inner.InnerTcpServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/6/3.
 */
@Component
public class InnerTcpServerMessageHandlerTest extends InnerTcpServerMessageHandler {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        String message = new String(received);
        System.out.println("Received TCP message: " + message);
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(("Echo: " + message).getBytes()));
    }

}
