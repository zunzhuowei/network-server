package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/6/13.
 */
public abstract class ClientMessageDispatcher implements InnerClientMessageHandler {

    /**
     * 处理消息，tcp, udp, websocket
     */
    public abstract void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 注意，业务层不要重写此方法。此方法给分发器使用
     */
    public void onMessage(ChannelHandlerContext ctx, Object msg) { }


}
