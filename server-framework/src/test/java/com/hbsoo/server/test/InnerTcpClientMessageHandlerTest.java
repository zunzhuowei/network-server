package com.hbsoo.server.test;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerTcpClientMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/6/4.
 */
@Component
public class InnerTcpClientMessageHandlerTest extends InnerTcpClientMessageHandler {

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final int msgType = decoder.readInt();
        switch (msgType) {
            case 1:
                System.out.println("TcpClientMessageHandlerTest onMessage 1");
                break;
            case 2:
                System.out.println("TcpClientMessageHandlerTest onMessage 2");
            }
        System.out.println("TcpClientMessageHandlerTest onMessage");
    }

}
