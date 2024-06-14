package com.hbsoo.server.action.client;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.ClientMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务器-客户端登录后，对服务器返回值处理
 * Created by zun.wei on 2024/6/6.
 */
@InnerClientMessageHandler(HBSMessageType.InnerMessageType.LOGIN)
public class InnerClientLoginAction extends ClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientLoginAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        int id = decoder.readInt();
        String loginServerTypeStr = decoder.readStr();
        int index = decoder.readInt();
        InnerClientSessionManager.innerLogin(loginServerTypeStr, id, ctx.channel(), index);
        logger.info("服务器返回的登录消息：InnerClientLoginAction login success,serverType[{}],id[{}],index[{}]", loginServerTypeStr, id, index);

    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        return decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.INT);
    }
}
