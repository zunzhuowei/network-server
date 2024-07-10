package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.utils.HttpRequestParser;
import io.netty.channel.ChannelHandlerContext;

/**
 * 接收网关转发到内网服务器中的http消息，
 * 再路由到相应的http处理器中
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(MessageType.Inside.GATEWAY_ROUTING_HTTP_TO_INNER_SERVER)
public class RoutingGatewayHttpMessageAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        HttpRequestParser parser = HttpRequestParser.parse(decoder);
        redirectAndSwitch2OuterHttp(ctx, parser);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

}
