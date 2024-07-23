package com.hbsoo.hall.action.http;

import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.DefaultHttpServerDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class StaticFilesAction extends DefaultHttpServerDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        String path = httpPacket.getPath();
        if (isStaticResource(path)) {
            ClassPathResource classPathResource = new ClassPathResource(path);
            try (InputStream inputStream = classPathResource.getInputStream()) {
                final int available = inputStream.available();
                byte[] bytes = new byte[available];
                inputStream.read(bytes);
                responseHtml(httpPacket, new String(bytes));
            } catch (IOException e) {
                e.printStackTrace();
                responseHtml(httpPacket, "404");
            }
        } else {
            responseHtml(httpPacket, "404");
        }
    }


    //判断请求路径是否为静态资源如：css,js，png等
    boolean isStaticResource(String path) {
        return path.endsWith(".html")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".gif")
                || path.endsWith(".ico")
                || path.endsWith(".jpeg")
                || path.endsWith(".svg")
                || path.endsWith(".ttf")
                || path.endsWith(".woff")
                || path.endsWith(".woff2")
                || path.endsWith(".eot")
                || path.endsWith(".otf")
                || path.endsWith(".map")
                ;
    }
}
