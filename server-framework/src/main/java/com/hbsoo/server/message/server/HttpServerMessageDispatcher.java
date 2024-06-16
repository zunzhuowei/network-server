package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by zun.wei on 2024/6/14.
 */
public abstract class HttpServerMessageDispatcher extends ServerMessageDispatcher {

    private GenericFutureListener<? extends Future<? super Void>> future;

    /**
     * 处理消息，http
     */
    public abstract void handle(ChannelHandlerContext ctx, HttpPackage httpPackage);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) { }

    public void responseJson(ChannelHandlerContext ctx, Object obj, Consumer<DefaultFullHttpResponse> consumer) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(obj);
        response(ctx, jsonStr.getBytes(CharsetUtil.UTF_8), "application/json; charset=UTF-8", consumer);
    }

    public void responseHtml(ChannelHandlerContext ctx, String html, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, html.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8", consumer);
    }

    public void responseXml(ChannelHandlerContext ctx, String xml, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, xml.getBytes(StandardCharsets.UTF_8), "text/xml; charset=UTF-8", consumer);
    }

    public void responseText(ChannelHandlerContext ctx, String text, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", consumer);
    }

    public void responseJpeg(ChannelHandlerContext ctx, byte[] bytes, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, bytes, "image/jpeg", consumer);
    }

    public void responseGif(ChannelHandlerContext ctx, byte[] bytes, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, bytes, "image/gif", consumer);
    }

    public void responsePng(ChannelHandlerContext ctx, byte[] bytes, Consumer<DefaultFullHttpResponse> consumer) {
        response(ctx, bytes, "image/png", consumer);
    }

    public void response(ChannelHandlerContext ctx, byte[] bytes, String contentType, Consumer<DefaultFullHttpResponse> consumer) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeBytes(bytes);
        //response.content().writeCharSequence(html, CharsetUtil.UTF_8);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (Objects.nonNull(consumer)) {
            consumer.accept(response);
        }
        if (Objects.nonNull(future)) {
            ctx.writeAndFlush(response).addListener(future);
        }else {
            ctx.writeAndFlush(response);
        }
        ctx.close();
    }

    protected HttpServerMessageDispatcher addResponseListener(GenericFutureListener<? extends Future<? super Void>> future) {
        this.future = future;
        return this;
    }


}
