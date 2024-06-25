package com.hbsoo.gateway.action;

import com.hbsoo.access.control.AccessLimit;
import com.hbsoo.gateway.entity.Genealogy;
import com.hbsoo.gateway.service.IGenealogyService;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@AccessLimit
@OuterServerMessageHandler(value = 0, uri = "/index2", protocol = Protocol.HTTP)
public class IndexAction2 extends HttpServerMessageDispatcher {


    @Autowired
    private IGenealogyService genealogyService;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        final List<Genealogy> genealogies = genealogyService.listAll();
        System.out.println("genealogies2 = " + genealogies);
        addResponseListener(future -> {
            if (future.isSuccess()) {
                System.out.println("writeAndFlush success");
            } else {
                System.out.println("writeAndFlush fail");
            }
        }).responseJson(ctx, genealogies, response -> {});

        forward2InnerServerUseSender(
                HBSPackage.Builder.withDefaultHeader()
                        .msgType(100).writeStr(genealogies.toString()),
                "hall",
                "",3);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
