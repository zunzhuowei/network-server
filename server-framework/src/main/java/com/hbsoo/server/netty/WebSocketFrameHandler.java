package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.Objects;

public final class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ServerMessageHandler websocketServerMessageDispatcher;

    public WebSocketFrameHandler(ServerMessageHandler websocketServerMessageDispatcher) {
        this.websocketServerMessageDispatcher = websocketServerMessageDispatcher;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        /*if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            ctx.channel().writeAndFlush(new TextWebSocketFrame("Server received your message: " + request));
        } else {
            throw new UnsupportedOperationException("Unsupported frame type: " + frame.getClass().getName());
        }*/
        if (Objects.nonNull(websocketServerMessageDispatcher)) {
            websocketServerMessageDispatcher.onMessage(ctx, frame);
        } else {
            final String s = frame.toString();
            System.err.println("WebSocketFrameHandler not config = " + s);
            ctx.close();
        }
    }
}
