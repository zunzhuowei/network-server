package com.hbsoo.hall.action.http;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OutsideMessageHandler(value = 0, uri = "/hall/joinRoom", protocol = Protocol.HTTP)
public class JoinRoomAction extends HttpServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        byte[] body = httpPacket.getBody();
        String page = "pages/game.html";
        String roomName = null;
        String username = null;
        boolean success = true;
        if (Objects.nonNull(body)) {
            String dataStr = new String(body);
            QueryStringDecoder decoder = new QueryStringDecoder("?" + dataStr);
            Map<String, List<String>> parameters = decoder.parameters();
            roomName = parameters.get("roomName").get(0);
            username = parameters.get("username").get(0);
            if (!StringUtils.hasLength(roomName)) {
                page = "pages/login.html";
                success = false;
            }
        } else {
            page = "pages/login.html";
            success = false;
        }
        ClassPathResource classPathResource = new ClassPathResource(page);
        try (InputStream inputStream = classPathResource.getInputStream()){
            final int available = inputStream.available();
            byte[] bytes = new byte[available];
            inputStream.read(bytes);
            String html = new String(bytes);
            if (success) {
                html = StringUtils.replace(html, "{{roomName}}", roomName);
                html = StringUtils.replace(html, "{{username}}", username);
                responseHtml(httpPacket, html);
            } else {
                Map<String, String> headers = new HashMap<>();
                headers.put(HttpHeaderNames.LOCATION.toString(), "/hall/login");
                outsideUserSessionManager.httpResponse(
                        headers,
                        null,
                        "text/html; charset=UTF-8",
                        httpPacket.getExtendBody(),
                        HttpResponseStatus.MOVED_PERMANENTLY);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
