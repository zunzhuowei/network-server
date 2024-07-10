package com.hbsoo.server.action.client;

import com.hbsoo.server.annotation.InsideClientMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.session.InsideClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务器-客户端登录后，对服务器返回值处理
 * Created by zun.wei on 2024/6/6.
 */
@InsideClientMessageHandler(MessageType.Inside.LOGIN)
public class InsideClientLoginAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideClientLoginAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        int id = decoder.readInt();
        String loginServerTypeStr = decoder.readStr();
        int index = decoder.readInt();
        InsideClientSessionManager.login(loginServerTypeStr, id, ctx.channel(), index);
        logger.info("服务器返回的登录消息：InsideClientLoginAction login success,serverType[{}],id[{}],index[{}]", loginServerTypeStr, id, index);

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.skipGetInt(NetworkPacket.DecodeSkip.INT, NetworkPacket.DecodeSkip.INT);
    }
}
