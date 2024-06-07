package com.hbsoo.server.action;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.server.InnerTcpServerMessageDispatcher;
import com.hbsoo.server.session.InnerServerSessionManager;
import com.hbsoo.server.session.ServerType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内部服务器登录接口
 * Created by zun.wei on 2024/6/6.
 */
@InnerServerMessageHandler(HBSMessageType.InnerMessageType.LOGIN)
public class InnerServerLoginAction extends InnerTcpServerMessageDispatcher {

    private static Logger logger = LoggerFactory.getLogger(InnerServerLoginAction.class);

    @Override
    public void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        final int serverId = decoder.readInt();
        final String serverTypeStr = decoder.readStr();
        final int id = decoder.readInt();
        final String loginServerTypeStr = decoder.readStr();
        InnerServerSessionManager.innerLogin(ServerType.valueOf(serverTypeStr), serverId, ctx.channel());
        //decoder.resetBodyReadOffset();
        final byte[] aPackage = HBSPackage.Builder.withDefaultHeader()
                .writeInt(HBSMessageType.InnerMessageType.LOGIN)
                .writeInt(id)
                .writeStr(loginServerTypeStr)
                .buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
        ctx.channel().writeAndFlush(buf);
        logger.info("接收到内部服务器登录消息：InnerServerLoginAction login success,serverType[{}],id[{}]", serverTypeStr, serverId);
    }

}
