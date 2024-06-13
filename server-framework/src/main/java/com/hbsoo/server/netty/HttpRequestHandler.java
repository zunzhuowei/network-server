package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.Objects;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerMessageHandler handler;

    public HttpRequestHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        /*if (request.uri().equals("/")) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, OK, Unpooled.wrappedBuffer("Welcome to the Netty Web Server".getBytes()));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.fireChannelRead(request.retain());
        }*/
        // 如果是websocket升级请求，则不走http请求处理
        if (request.headers().contains(HttpHeaderNames.UPGRADE, "websocket", true) &&
                request.headers().contains(HttpHeaderNames.CONNECTION, "Upgrade", true)) {
            ctx.fireChannelRead(request.retain());
            return;
        }
        handler.onMessage(ctx, request);
    }
}