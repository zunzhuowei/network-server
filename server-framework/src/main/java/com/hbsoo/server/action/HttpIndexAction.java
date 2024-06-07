package com.hbsoo.server.action;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.OuterHttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 测试请求，需要在NetworkServerAutoConfiguration中注入bean对象，否则springboot扫描不到
 * Created by zun.wei on 2024/6/7.
 */
@OuterServerMessageHandler(value = 0, uri = "/")
public class HttpIndexAction extends OuterHttpServerMessageDispatcher {

    @Override
    public void onMessage(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        System.out.println("HttpIndexAction" + httpPackage);
        final FullHttpRequest fullHttpRequest = httpPackage.getFullHttpRequest();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(fullHttpRequest.protocolVersion(), HttpResponseStatus.OK);
        response.content().writeCharSequence("hello world", io.netty.util.CharsetUtil.UTF_8);
        try {
            ctx.writeAndFlush(response).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
