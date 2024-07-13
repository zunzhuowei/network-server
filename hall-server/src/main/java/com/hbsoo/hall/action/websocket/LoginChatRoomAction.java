package com.hbsoo.hall.action.websocket;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OutsideMessageHandler(100)
public class LoginChatRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(LoginChatRoomAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        ExtendBody extendBody = decoder.readExtendBody();
        String channelId = extendBody.getUserChannelId();
        String username = decoder.readStr();
        int userId = Math.abs(username.hashCode());
        logger.info("login chat room username:{}，channelId:{}，userId:{}", username, channelId, userId);
        //通知客户端登录成功
        NetworkPacket.Builder builder = decoder.toBuilder().writeInt(userId).writeStr(Permission.USER.name());
        builder.sendTcpTo(ctx.channel());
        //加入房间
        forward2InsideServerUseSender(builder, "room", userId);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
