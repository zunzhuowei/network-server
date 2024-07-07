package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收外部服务器登出后，同步删除本服会话信息
 * Created by zun.wei on 2024/6/6.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.LOGOUT_SYNC)
public class InnerServerOuterUserSessionLogoutSyncAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerOuterUserSessionLogoutSyncAction.class);
    @Autowired
    private OuterUserSessionManager outerUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        long id = decoder.readLong();
        logger.debug("InnerServerUserSessionLogoutSyncAction id:{}", id);
        outerUserSessionManager.logout(id);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.skipGetLong(HBSPackage.DecodeSkip.INT);
    }
}
