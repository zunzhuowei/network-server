package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/14.
 */
public abstract class HttpServerMessageDispatcher extends ServerMessageDispatcher {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    /**
     * 处理消息，http
     */
    public abstract void handle(ChannelHandlerContext ctx, HttpPackage httpPackage);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
    }

    public void responseJson(ChannelHandlerContext ctx, HttpPackage httpPackage, Object obj) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String jsonStr = gson.toJson(obj);
        response(ctx, httpPackage, jsonStr.getBytes(CharsetUtil.UTF_8), "application/json; charset=UTF-8", null);
    }

    public void responseHtml(ChannelHandlerContext ctx, HttpPackage httpPackage, String html) {
        response(ctx, httpPackage, html.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8", null);
    }

    public void responseXml(ChannelHandlerContext ctx, HttpPackage httpPackage, String xml) {
        response(ctx, httpPackage, xml.getBytes(StandardCharsets.UTF_8), "text/xml; charset=UTF-8", null);
    }

    public void responseText(ChannelHandlerContext ctx, HttpPackage httpPackage, String text) {
        response(ctx, httpPackage, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", null);
    }

    public void responseJpeg(ChannelHandlerContext ctx, HttpPackage httpPackage, byte[] bytes) {
        response(ctx, httpPackage, bytes, "image/jpeg", null);
    }

    public void responseGif(ChannelHandlerContext ctx, HttpPackage httpPackage, byte[] bytes) {
        response(ctx, httpPackage, bytes, "image/gif", null);
    }

    public void responsePng(ChannelHandlerContext ctx, HttpPackage httpPackage, byte[] bytes) {
        response(ctx, httpPackage, bytes, "image/png", null);
    }

    public void response(ChannelHandlerContext ctx, HttpPackage httpPackage,
                         byte[] bytes, String contentType,
                         GenericFutureListener<? extends Future<? super Void>> future) {
        // 如果是内部服务转发过来的消息则转发回去
        String outsideUserId = httpPackage.getHeaders().get("outsideUserId");
        if (StringUtils.hasLength(outsideUserId)) {
            outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.HTTP, bytes, contentType, Long.parseLong(outsideUserId));
            return;
        }
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeBytes(bytes);
        //response.content().writeCharSequence(html, CharsetUtil.UTF_8);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        //response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (Objects.nonNull(future)) {
            ctx.writeAndFlush(response).addListener(future).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }


    protected void forwardOuterHttpMsg2InnerServer(ChannelHandlerContext ctx, HttpPackage httpPackage, String serverType, int msgType) {
        String uri = httpPackage.getUri();
        int index = uri.indexOf("?");
        String path = index < 0 ? uri : uri.substring(0, index);
        long id = snowflakeIdGenerator.generateId();
        UserSession userSession = new UserSession();
        userSession.setId(id);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        userSession.setUdp(false);
        outsideUserSessionManager.login(id, userSession);

        HttpHeaders headers = httpPackage.getHeaders();
        Map<String, String> headersMap = new HashMap<>();
        for (String name : headers.names()) {
            String value = headers.get(name);
            headersMap.put(name, value);
        }
        headersMap.put("outsideUserId", userSession.getId().toString());
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        NetworkPacket.Builder msgBuilder = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(msgType)
                .writeStr(gson.toJson(userSession))
                .writeStr(uri)
                .writeStr(path)
                .writeStr(httpPackage.getMethod())
                .writeStr(gson.toJson(httpPackage.getParameters()))
                .writeBytes(httpPackage.getBody() == null ? new byte[0] : httpPackage.getBody())
                .writeStr(gson.toJson(headersMap));
        forward2InnerServer(msgBuilder, serverType, ctx.channel().id().asLongText());
    }

}
