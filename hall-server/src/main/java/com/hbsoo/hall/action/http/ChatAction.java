package com.hbsoo.hall.action.http;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(value = 0, uri = "/hall/chat", protocol = Protocol.HTTP)
public class ChatAction extends HttpServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        byte[] body = httpPackage.getBody();
        String dataStr = new String(body);
        QueryStringDecoder decoder = new QueryStringDecoder("?" + dataStr);
        Map<String, List<String>> parameters = decoder.parameters();
        String username = parameters.get("username").get(0);
        String page = "pages/chat.html";
        boolean success = true;
        if (!StringUtils.hasLength(username)) {
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
            responseHtml(ctx, httpPackage, html);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }



}
