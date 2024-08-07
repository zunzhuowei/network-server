package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServerMessageHandler handler;

    public WebSocketFrameHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        handler.onMessage(ctx, frame);
    }

}
