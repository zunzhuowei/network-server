package com.hbsoo.hall.action;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(value = 0, uri = "/hall/chat", protocol = Protocol.HTTP)
public class ChatLoginAction extends HttpServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        ClassPathResource classPathResource = new ClassPathResource("pages/login.html");
        try (InputStream inputStream = classPathResource.getInputStream()){
            final int available = inputStream.available();
            byte[] bytes = new byte[available];
            inputStream.read(bytes);
            responseHtml(ctx, httpPackage, new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }



}
