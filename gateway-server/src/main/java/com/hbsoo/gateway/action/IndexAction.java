package com.hbsoo.gateway.action;

//import com.hbsoo.access.control.AccessLimit;
import com.hbsoo.gateway.entity.Genealogy;
import com.hbsoo.gateway.service.IGenealogyService;
import com.hbsoo.message.queue.QueueMessageSender;
import com.hbsoo.permisson.PermissionAuth;
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
@PermissionAuth(permission = {})
//@AccessLimit(userRateSize = 1, globalRateSize = 2)
@OuterServerMessageHandler(value = 0, uri = "/index", protocol = Protocol.HTTP)
public class IndexAction extends HttpServerMessageDispatcher {


    @Autowired
    private IGenealogyService genealogyService;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {
        final List<Genealogy> genealogies = genealogyService.listAll();
        //System.out.println("genealogies = " + genealogies);
        addResponseListener(future -> {
            if (future.isSuccess()) {
                //System.out.println("writeAndFlush success");
            } else {
                System.out.println("writeAndFlush fail");
            }
        }).responseJson(ctx, genealogies, response -> {});

        forward2InnerServerUseSender(
                HBSPackage.Builder.withDefaultHeader()
                        .msgType(100).writeStr(genealogies.toString()),
                "hall",
                "",3);

        QueueMessageSender.publish("hall", "test", genealogies.toString());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
