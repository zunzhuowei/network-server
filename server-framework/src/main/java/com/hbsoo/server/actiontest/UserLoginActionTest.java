package com.hbsoo.server.actiontest;

import com.google.gson.Gson;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.TextWebSocketPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/12.
 */
@OuterServerMessageHandler(HBSMessageType.OuterMessageType.LOGIN)
public class UserLoginActionTest extends ServerMessageDispatcher {

    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final String dataJson = decoder.readStr();
        System.out.println("UserLoginActionTest dataJson = " + dataJson);
        Gson gson = new Gson();
        TextWebSocketPackage textWebSocketPackage = gson.fromJson(dataJson, TextWebSocketPackage.class);
        Map<String, Object> data = textWebSocketPackage.getData();
        UserSession userSession = gson.fromJson(gson.toJson(data), UserSession.class);
        userSession.setBelongServer(NowServer.getServerInfo());
        userSession.setChannel(ctx.channel());
        outerSessionManager.loginAndSyncAllServer(userSession.getId(), userSession);

        //redirectMessage();
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {

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
