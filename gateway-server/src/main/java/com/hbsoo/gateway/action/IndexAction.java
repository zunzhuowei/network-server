package com.hbsoo.gateway.action;

//import com.hbsoo.access.control.AccessLimit;
import com.hbsoo.message.queue.QueueMessageSender;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth(permission = {})
//@AccessLimit(userRateSize = 1, globalRateSize = 2)
@OutsideMessageHandler(value = 0, uri = "/index", protocol = Protocol.HTTP)
public class IndexAction extends HttpServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        List<String> genealogies = new ArrayList<>();
        genealogies.add("zun");
        responseJson(httpPacket, genealogies);

        forward2InsideServerUseSender(
                NetworkPacket.Builder.withDefaultHeader()
                        .msgType(100).writeStr(genealogies.toString()),
                "hall",
                "",3);

        QueueMessageSender.publish("hall", "test", genealogies.toString());
    }

}
