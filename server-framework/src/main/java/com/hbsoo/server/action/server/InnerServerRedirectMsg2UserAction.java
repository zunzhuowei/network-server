package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.session.UserSessionProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收内部服务器的消息，转发给登录的外部客户
 * Created by zun.wei on 2024/6/6.
 */
@InnerServerMessageHandler(HBSMessageType.InnerMessageType.REDIRECT)
public class InnerServerRedirectMsg2UserAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerRedirectMsg2UserAction.class);
    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final long id = decoder.readLong();
        String protocolStr = decoder.readStr();
        byte[] innerPackage = decoder.readBytes();
        UserSessionProtocol protocol = UserSessionProtocol.valueOf(protocolStr);
        logger.debug("InnerServerRedirectMsg2UserAction id:{} protocol:{}", id, protocol);
        outerSessionManager.sendMsg2User(protocol, innerPackage, id);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {

    }

    @Override
    public Object threadKey(HBSPackage.Decoder decoder) {
        return decoder.skipGetLong(HBSPackage.DecodeSkip.INT);
    }
}
