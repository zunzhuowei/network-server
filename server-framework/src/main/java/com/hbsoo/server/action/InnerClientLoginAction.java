package com.hbsoo.server.action;

import com.hbsoo.server.annotation.InnerClientMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.client.InnerTcpClientMessageDispatcher;
import com.hbsoo.server.session.InnerClientSessionManager;
import com.hbsoo.server.session.ServerType;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务器-客户端登录后，对服务器返回值处理
 * Created by zun.wei on 2024/6/6.
 */
@InnerClientMessageHandler(HBSMessageType.InnerMessageType.LOGIN)
public class InnerClientLoginAction extends InnerTcpClientMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerClientLoginAction.class);


    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        int id = decoder.readInt();
        String loginServerTypeStr = decoder.readStr();
        int index = decoder.readInt();
        InnerClientSessionManager.innerLogin(ServerType.valueOf(loginServerTypeStr), id, ctx.channel(), index);
        logger.info("服务器返回的登录消息：InnerClientLoginAction login success,serverType[{}],id[{}],index[{}]", loginServerTypeStr, id, index);

    }

}
