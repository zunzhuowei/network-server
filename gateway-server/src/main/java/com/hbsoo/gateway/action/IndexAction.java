package com.hbsoo.gateway.action;

import com.hbsoo.gateway.entity.Genealogy;
import com.hbsoo.gateway.service.IGenealogyService;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(value = 0, uri = "/index", protocol = Protocol.HTTP)
public class IndexAction extends HttpServerMessageDispatcher {


    @Autowired
    private IGenealogyService genealogyService;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        final List<Genealogy> genealogies = genealogyService.listAll();
        System.out.println("genealogies = " + genealogies);
        addResponseListener(future -> {
            if (future.isSuccess()) {
                System.out.println("writeAndFlush success");
            } else {
                System.out.println("writeAndFlush fail");
            }
        }).responseJson(ctx, genealogies, response -> {});

        redirect2InnerServer(
                HBSPackage.Builder.withDefaultHeader()
                        .msgType(100).writeStr(genealogies.toString()),
                "hall",
                "");
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
