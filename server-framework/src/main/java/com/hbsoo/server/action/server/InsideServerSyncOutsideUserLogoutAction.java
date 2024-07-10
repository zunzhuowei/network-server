package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收外部服务器登出后，同步删除本服会话信息
 * Created by zun.wei on 2024/6/6.
 */
@InsideServerMessageHandler(MessageType.Inside.LOGOUT_SYNC)
public class InsideServerSyncOutsideUserLogoutAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerSyncOutsideUserLogoutAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long id = decoder.readLong();
        logger.debug("InsideServerSyncOutsideUserLogoutAction id:{}", id);
        outsideUserSessionManager.logout(id);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.skipGetLong(NetworkPacket.DecodeSkip.INT);
    }
}
