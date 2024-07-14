package com.hbsoo.gateway.action;

import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.DefaultHttpServerDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Created by zun.wei on 2024/7/5.
 */
@Component
public class HttpMessageRoutingAction extends DefaultHttpServerDispatcher {

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        String uri = httpPacket.getUri();
        final int index = uri.indexOf("?");
        String path = index < 0 ? uri : uri.substring(0, index);
        int msgType = MessageType.Inside.GATEWAY_ROUTING_MESSAGE_TO_INNER_SERVER;
        if (StringUtils.startsWith(path, "/hall")) {
            forwardOutsideHttpMsg2InsideServer(ctx, httpPacket, "hall", msgType);
            return;
        }
        if (StringUtils.startsWith(path, "/room")) {
            forwardOutsideHttpMsg2InsideServer(ctx, httpPacket, "room", msgType);
            return;
        }
        responseHtml(httpPacket, "404");
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
