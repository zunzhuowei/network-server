package com.hbsoo.server.actiontest;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zun.wei on 2024/6/12.
 */
@OuterServerMessageHandler(value = HBSMessageType.Outer.LOGIN, protocol = Protocol.UDP)
public class UdpProtocolActionTest extends ServerMessageDispatcher {

    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String sendHost = decoder.readStr();
        final int sendPort = decoder.readInt();
        final String dataJson = decoder.readStr();

        HBSPackage.Builder.withHeader(HBSPackage.UDP_HEADER)
                .msgType(1).writeStr(dataJson)
                .buildAndSendUdpTo(ctx.channel(), sendHost, sendPort);
        redirectAndSwitchProtocol(ctx,
                ProtocolType.INNER_WEBSOCKET,
                HBSPackage.Builder.withHeader(HBSPackage.TCP_HEADER)
                .msgType(HBSMessageType.Inner.LOGOUT)
                .writeStr("hahahaha").toDecoder()
        );
        /*System.out.println("UserLoginActionTest dataJson = " + dataJson);
        Gson gson = new Gson();
        TextWebSocketPackage textWebSocketPackage = gson.fromJson(dataJson, TextWebSocketPackage.class);
        Map<String, Object> data = textWebSocketPackage.getData();
        UserSession userSession = gson.fromJson(gson.toJson(data), UserSession.class);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        outerSessionManager.loginAndSyncAllServer(userSession.getId(), userSession);*/
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
    /*
    {
    "msgType":1,
    "data":{
        "id":1,
        "name":"zhangsan",
        "token":"dsxxdaee23fsa"
    }
}
     */
}
