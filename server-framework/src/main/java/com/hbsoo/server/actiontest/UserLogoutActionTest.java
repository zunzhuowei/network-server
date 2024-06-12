package com.hbsoo.server.actiontest;

import com.google.gson.Gson;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.TextWebSocketPackage;
import com.hbsoo.server.message.server.OuterWebsocketServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/12.
 */
@OuterServerMessageHandler(HBSMessageType.OuterMessageType.LOGOUT)
public class UserLogoutActionTest extends OuterWebsocketServerMessageDispatcher {

    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String dataJson = decoder.readStr();
        System.out.println("UserLogoutActionTest dataJson = " + dataJson);
        Gson gson = new Gson();
        TextWebSocketPackage textWebSocketPackage = gson.fromJson(dataJson, TextWebSocketPackage.class);
        Map<String, Object> data = textWebSocketPackage.getData();
        UserSession userSession = gson.fromJson(gson.toJson(data), UserSession.class);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        outerSessionManager.logoutAndSyncAllServer(userSession.getId());
    }
    /*
    {
    "msgType":2,
    "data":{
        "id":1,
        "name":"zhangsan",
        "token":"dsxxdaee23fsa"
    }
     */
}
