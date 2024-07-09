package com.hbsoo.gateway.action.inner;

import com.alibaba.fastjson.JSON;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 测试同步消息响应
 * Created by zun.wei on 2024/7/9.
 */
@InnerServerMessageHandler(1001)
public class getUserSessionAction extends ServerMessageDispatcher {

    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String message = decoder.readStr();
        long userId = decoder.readLong();
        long messageId = decoder.readLong();
        UserSession userSession = outerUserSessionManager.getUserSession(userId);
        String jsonString = JSON.toJSONString(userSession);
        HBSPackage.Builder.withDefaultHeader()
                .msgType(1001)
                .writeStr(jsonString)
                .writeLong(messageId).sendTcpTo(ctx.channel());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return null;
    }
}
