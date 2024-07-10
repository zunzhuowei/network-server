package com.hbsoo.hall.action.http;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OutsideMessageHandler(value = 0, uri = "/hall/chat", protocol = Protocol.HTTP)
public class ChatAction extends HttpServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        byte[] body = httpPacket.getBody();
        String page = "pages/chat.html";
        String username = null;
        boolean success = true;
        if (Objects.nonNull(body)) {
            String dataStr = new String(body);
            QueryStringDecoder decoder = new QueryStringDecoder("?" + dataStr);
            Map<String, List<String>> parameters = decoder.parameters();
            username = parameters.get("username").get(0);
            if (!StringUtils.hasLength(username)) {
                page = "pages/login.html";
                success = false;
            }
        } else {
            page = "pages/login.html";
            success = false;
        }
        ClassPathResource classPathResource = new ClassPathResource(page);
        try (InputStream inputStream = classPathResource.getInputStream()){
            int available = inputStream.available();
            byte[] bytes = new byte[available];
            inputStream.read(bytes);
            String html = new String(bytes);
            if (success) {
                html = StringUtils.replace(html, "{{username}}", username);
            }
            responseHtml(ctx, httpPacket, html);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }



}
