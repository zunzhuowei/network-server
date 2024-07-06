package com.hbsoo.gateway.action;

import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
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
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        String uri = httpPackage.getUri();
        final int index = uri.indexOf("?");
        String path = index < 0 ? uri : uri.substring(0, index);
        if (StringUtils.startsWith(path, "/hall")) {
            forwardOuterHttpMsg2InnerServer(ctx, httpPackage, "hall", HBSMessageType.Inner.GATEWAY_ROUTING_HTTP_TO_INNER_SERVER);
            return;
        }
        if (StringUtils.startsWith(path, "/room")) {
            forwardOuterHttpMsg2InnerServer(ctx, httpPackage, "room", HBSMessageType.Inner.GATEWAY_ROUTING_HTTP_TO_INNER_SERVER);
            return;
        }
        responseHtml(ctx, httpPackage, "404");
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }

}
