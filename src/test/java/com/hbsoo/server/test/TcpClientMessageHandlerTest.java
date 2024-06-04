package com.hbsoo.server.test;

import com.hbsoo.server.message.client.TcpClientMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/6/4.
 */
@Component
public class TcpClientMessageHandlerTest extends TcpClientMessageHandler {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        super.onMessage(ctx, msg);
    }

}
