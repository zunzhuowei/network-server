package com.hbsoo.server.actiontest;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 测试请求，需要在NetworkServerAutoConfiguration中注入bean对象，否则springboot扫描不到
 * Created by zun.wei on 2024/6/7.
 */
@OuterServerMessageHandler(value = 0, uri = "/")
public class HttpIndexActionTest extends ServerMessageDispatcher {

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {}

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        System.out.println("HttpIndexAction" + httpPackage);
        final FullHttpRequest fullHttpRequest = httpPackage.getFullHttpRequest();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(fullHttpRequest.protocolVersion(), HttpResponseStatus.OK);
        response.content().writeCharSequence("hello world", io.netty.util.CharsetUtil.UTF_8);
        try {
            //response.content().writeBytes("404".getBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            //response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
            //response.headers().set(HttpHeaderNames.SERVER, "Netty");
            //response.headers().set(HttpHeaderNames.DATE, "Wed, 03 Mar 2021 05:06:07 GMT");
            //response.headers().set(HttpHeaderNames.CONTENT_ENCODING, "gzip");
            //response.headers().set(HttpHeaderNames.CONTENT_LANGUAGE, "zh-CN");
            //response.headers().set(HttpHeaderNames.CONTENT_LOCATION, "/");
            //response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes 0-1023/1024");
            //response.headers().set(HttpHeaderNames.CONTENT_SECURITY_POLICY, "default-src 'self'");
            //response.headers().set(HttpHeaderNames.COOKIE, "text/plain; charset=UTF-8");
            //response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"filename.txt\"");
            //response.headers().set(HttpHeaderNames.CONTENT_TRANSFER_ENCODING, "base64");
            //response.headers().set(HttpHeaderNames.CONTENT_MD5, "Q2hlY2sgSW50ZWdyaXR5IQ==");
            ctx.writeAndFlush(response).sync();
            ctx.close();

            //java.lang.IllegalStateException: cannot send more responses than requests
            //redirectMessage(ctx, httpPackage.getFullHttpRequest());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
