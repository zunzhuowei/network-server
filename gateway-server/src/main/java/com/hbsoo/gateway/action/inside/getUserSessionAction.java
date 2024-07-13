package com.hbsoo.gateway.action.inside;

import com.alibaba.fastjson.JSON;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 测试同步消息响应
 * Created by zun.wei on 2024/7/9.
 */
@InsideServerMessageHandler(1001)
public class getUserSessionAction extends ServerMessageDispatcher {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        UserSession userSession1 = extendBody.getUserSession();
        String message = decoder.readStr();
        System.out.println("getUserSessionAction message = " + message);
        UserSession userSession = outsideUserSessionManager.getUserSession(userSession1.getId());
        String jsonString = JSON.toJSONString(userSession);
        NetworkPacket.Builder.withDefaultHeader()
                .msgType(1001)
                .writeStr(jsonString)
                .writeExtendBodyMode()
                //expandBody 必须写入，根据里面的msgId 找到对应的请求
                .writeObj(extendBody)
                .sendTcpTo(ctx.channel());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
