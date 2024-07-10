package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
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
    public abstract void handle(ChannelHandlerContext ctx, HttpPacket httpPacket);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
    }

    public void responseJson(ChannelHandlerContext ctx, HttpPacket httpPacket, Object obj) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String jsonStr = gson.toJson(obj);
        response(ctx, httpPacket, jsonStr.getBytes(CharsetUtil.UTF_8), "application/json; charset=UTF-8", null);
    }

    public void responseHtml(ChannelHandlerContext ctx, HttpPacket httpPacket, String html) {
        response(ctx, httpPacket, html.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8", null);
    }

    public void responseXml(ChannelHandlerContext ctx, HttpPacket httpPacket, String xml) {
        response(ctx, httpPacket, xml.getBytes(StandardCharsets.UTF_8), "text/xml; charset=UTF-8", null);
    }

    public void responseText(ChannelHandlerContext ctx, HttpPacket httpPacket, String text) {
        response(ctx, httpPacket, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", null);
    }

    public void responseJpeg(ChannelHandlerContext ctx, HttpPacket httpPacket, byte[] bytes) {
        response(ctx, httpPacket, bytes, "image/jpeg", null);
    }

    public void responseGif(ChannelHandlerContext ctx, HttpPacket httpPacket, byte[] bytes) {
        response(ctx, httpPacket, bytes, "image/gif", null);
    }

    public void responsePng(ChannelHandlerContext ctx, HttpPacket httpPacket, byte[] bytes) {
        response(ctx, httpPacket, bytes, "image/png", null);
    }

    public void response(ChannelHandlerContext ctx, HttpPacket httpPacket,
                         byte[] bytes, String contentType,
                         GenericFutureListener<? extends Future<? super Void>> future) {
        // 如果是内部服务转发过来的消息则转发回去
        String outsideUserId = httpPacket.getHeaders().get("outsideUserId");
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


    protected void forwardOutsideHttpMsg2InsideServer(ChannelHandlerContext ctx, HttpPacket httpPacket, String serverType, int msgType) {
        String uri = httpPacket.getUri();
        int index = uri.indexOf("?");
        String path = index < 0 ? uri : uri.substring(0, index);
        long id = snowflakeIdGenerator.generateId();
        UserSession userSession = new UserSession();
        userSession.setId(id);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        userSession.setUdp(false);
        outsideUserSessionManager.login(id, userSession);

        HttpHeaders headers = httpPacket.getHeaders();
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
                .writeStr(httpPacket.getMethod())
                .writeStr(gson.toJson(httpPacket.getParameters()))
                .writeBytes(httpPacket.getBody() == null ? new byte[0] : httpPacket.getBody())
                .writeStr(gson.toJson(headersMap));
        forward2InsideServer(msgBuilder, serverType, ctx.channel().id().asLongText());
    }

}
