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
        //1.执行登录流程，同步到到网关服。
        //2.判断聊天房间是否存在
        //3.存在则进入，不存在则创建；进入聊天房间
        //4.发送聊天小则走房间服务。
        int userId = Math.abs(username.hashCode());
        logger.info("login chat room username:{}，channelId:{}，userId:{}", username, channelId, userId);
        decoder.toBuilder().writeInt(userId).sendTcpTo(ctx.channel());
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.readStr();
    }
}
