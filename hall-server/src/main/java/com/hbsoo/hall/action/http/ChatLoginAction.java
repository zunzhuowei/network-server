package com.hbsoo.hall.action.http;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OutsideMessageHandler(value = 0, uri = "/hall/login", protocol = Protocol.HTTP)
public class ChatLoginAction extends HttpServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        ClassPathResource classPathResource = new ClassPathResource("pages/login.html");
        try (InputStream inputStream = classPathResource.getInputStream()){
            final int available = inputStream.available();
            byte[] bytes = new byte[available];
            inputStream.read(bytes);
            responseHtml(httpPacket, new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
