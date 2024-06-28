package com.hbsoo.gateway.service.impl;

import com.alibaba.fastjson.JSON;
import com.hbsoo.server.client.outer.WebsocketOuterUserLoginAuthenticator;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/22.
 */
@Service
public class GatewayUserLoginAuthenticator extends WebsocketOuterUserLoginAuthenticator {


    @Override
    public Long authentication(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String s = decoder.readStr();
        System.out.println("s = " + s);
        Map map = JSON.parseObject(s, Map.class);
        final Object data = map.get("data");
        System.out.println("data = " + data);
        Integer id = (Integer) ((Map) data).get("id");
        HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Outer.LOGIN)
                .writeInt(id)
                .buildAndSendTextWebSocketTo(ctx.channel());
        return id.longValue();
    }

    @Override
    public void logoutCallback(Long id) {
        System.out.println("id = " + id);
    }

}
