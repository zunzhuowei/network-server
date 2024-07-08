package com.hbsoo.hall.action.websocket;

import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OuterServerMessageHandler(100)
public class LoginChatRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(LoginChatRoomAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        String username = decoder.readStr();
        String channelId = decoder.readStr();
        int userId = Math.abs(username.hashCode());
        logger.info("login chat room username:{}，channelId:{}，userId:{}", username, channelId, userId);
        //通知客户端登录成功
        HBSPackage.Builder builder = decoder.toBuilder().writeInt(userId);
        builder.sendTcpTo(ctx.channel());
        //加入房间
        forward2InnerServerUseSender(builder, "room", userId);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readStr();
    }
}
