package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 接收内部服务器的消息，转发给登录的外部客户
 * Created by zun.wei on 2024/6/6.
 */
@InsideServerMessageHandler(MessageType.Inside.REDIRECT)
public class InsideServerRedirectMsg2OutsideUserAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerRedirectMsg2OutsideUserAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        long id = decoder.readLong();
        String protocolStr = decoder.readStr();
        String contentType = decoder.readStr();
        byte[] innerPackage = decoder.readBytes();
        OutsideUserProtocol protocol = OutsideUserProtocol.valueOf(protocolStr);
        logger.debug("InsideServerRedirectMsg2OutsideUserAction id:{} protocol:{}", id, protocol);
        outsideUserSessionManager.sendMsg2User(protocol, innerPackage, contentType, id);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.skipGetLong(NetworkPacket.DecodeSkip.INT);
    }
}
