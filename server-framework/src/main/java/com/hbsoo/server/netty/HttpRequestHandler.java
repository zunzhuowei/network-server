package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.util.Objects;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerMessageHandler httpServerMessageDispatcher;

    public HttpRequestHandler(ServerMessageHandler httpServerMessageDispatcher) {
        this.httpServerMessageDispatcher = httpServerMessageDispatcher;
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
        if (Objects.nonNull(httpServerMessageDispatcher)) {
            httpServerMessageDispatcher.onMessage(ctx, request);
        } else {
            final String s = request.toString();
            System.err.println("HttpMessageHandler not config = " + s);
            ctx.close();
        }
    }
}