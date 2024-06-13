package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Objects;

public final class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ServerMessageHandler handler;

    public WebSocketFrameHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        /*if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            ctx.channel().writeAndFlush(new TextWebSocketFrame("Server received your message: " + request));
        } else {
            throw new UnsupportedOperationException("Unsupported frame type: " + frame.getClass().getName());
        }*/
        handler.onMessage(ctx, frame);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("WebSocketFrameHandler channelInactive");
        ctx.close();

        // 注销登录
        AttributeKey<Long> idAttr = AttributeKey.valueOf("id");
        Attribute<Long> attr = ctx.channel().attr(idAttr);
        Long id = attr.get();
        if (Objects.nonNull(id)) {
            OuterSessionManager manager = SpringBeanFactory.getBean(OuterSessionManager.class);
            System.out.println("channelInactive id = " + id);
            manager.logoutAndSyncAllServer(id);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("WebSocketFrameHandler channelActive");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("WebSocketFrameHandler channelRegistered");
    }
}
