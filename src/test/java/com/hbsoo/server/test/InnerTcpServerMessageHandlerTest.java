package com.hbsoo.server.test;

import com.hbsoo.server.message.HBSPackage;
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
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {

    }

}
