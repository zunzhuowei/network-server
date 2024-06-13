package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收外部服务器登出后，同步删除本服会话信息
 * Created by zun.wei on 2024/6/6.
 */
@InnerServerMessageHandler(HBSMessageType.InnerMessageType.LOGOUT_SYNC)
public class InnerServerUserSessionLogoutSyncAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerUserSessionLogoutSyncAction.class);
    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long id = decoder.readLong();
        logger.debug("InnerServerUserSessionLogoutSyncAction id:{}", id);
        outerSessionManager.logout(id);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {

    }

    @Override
    public Object threadKey(HBSPackage.Decoder decoder) {
        return decoder.skipGetLong(HBSPackage.DecodeSkip.INT);
    }
}
