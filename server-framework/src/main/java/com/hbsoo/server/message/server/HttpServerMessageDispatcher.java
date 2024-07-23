package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    public void responseJson(HttpPacket httpPacket, Object obj) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String jsonStr = gson.toJson(obj);
        response(httpPacket, jsonStr.getBytes(CharsetUtil.UTF_8), "application/json; charset=UTF-8", null);
    }

    public void responseHtml(HttpPacket httpPacket, String html) {
        response(httpPacket, html.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8", null);
    }

    public void responseXml(HttpPacket httpPacket, String xml) {
        response(httpPacket, xml.getBytes(StandardCharsets.UTF_8), "text/xml; charset=UTF-8", null);
    }

    public void responseText(HttpPacket httpPacket, String text) {
        response(httpPacket, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8", null);
    }

    public void responseJpeg(HttpPacket httpPacket, byte[] bytes) {
        response(httpPacket, bytes, "image/jpeg", null);
    }

    public void responseGif(HttpPacket httpPacket, byte[] bytes) {
        response(httpPacket, bytes, "image/gif", null);
    }

    public void responsePng(HttpPacket httpPacket, byte[] bytes) {
        response(httpPacket, bytes, "image/png", null);
    }

    public void response(HttpPacket httpPacket,
                         byte[] bytes, String contentType,
                         GenericFutureListener<? extends Future<? super Void>> future) {
        response(null, httpPacket, bytes, contentType, future);
    }

    public void response(Map<String,String> headers, HttpPacket httpPacket,
                         byte[] bytes, String contentType,
                         GenericFutureListener<? extends Future<? super Void>> future) {
        ExtendBody extendBody = httpPacket.getExtendBody();
        outsideUserSessionManager.httpResponse(headers, bytes, contentType, extendBody, future);
    }

    protected void forwardOutsideHttpMsg2InsideServer(ChannelHandlerContext ctx, HttpPacket httpPacket, String serverType, int msgType) {
        String uri = httpPacket.getUri();
        int index = uri.indexOf("?");
        String path = index < 0 ? uri : uri.substring(0, index);
        ExtendBody extendBody = httpPacket.getExtendBody();
        HttpHeaders headers = httpPacket.getHeaders();
        Map<String, String> headersMap = new HashMap<>();
        for (String name : headers.names()) {
            String value = headers.get(name);
            headersMap.put(name, value);
        }
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        NetworkPacket.Builder msgBuilder = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(msgType)
                .writeStr(uri)
                .writeStr(path)
                .writeStr(httpPacket.getMethod())
                .writeStr(gson.toJson(httpPacket.getParameters()))
                .writeBytes(httpPacket.getBody() == null ? new byte[0] : httpPacket.getBody())
                .writeStr(gson.toJson(headersMap))
                .writeExtendBodyMode()
                .writeObj(extendBody);
        forward2InsideServer(msgBuilder, serverType, ctx.channel().id().asLongText());
    }

}
