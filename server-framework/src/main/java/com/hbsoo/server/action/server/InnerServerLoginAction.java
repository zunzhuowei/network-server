package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
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
public class InnerServerLoginAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerLoginAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        int serverId = decoder.readInt();
        String serverTypeStr = decoder.readStr();
        int index = decoder.readInt();
        int id = decoder.readInt();
        String loginServerTypeStr = decoder.readStr();
        //InnerServerSessionManager.innerLogin(ServerType.valueOf(serverTypeStr), serverId, ctx.channel(), index);
        //decoder.resetBodyReadOffset();
        byte[] aPackage = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.InnerMessageType.LOGIN)
                .writeInt(id)
                .writeStr(loginServerTypeStr)
                .writeInt(index)//客户端编号
                .buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
        ctx.channel().writeAndFlush(buf);
        logger.info("接收到内部服务器登录消息：InnerServerLoginAction login success,serverType[{}],id[{}],id[{}]", serverTypeStr, serverId, index);

    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPackage httpPackage) {

    }

    @Override
    public Object threadKey(HBSPackage.Decoder decoder) {
        //服务器id + 客户端编号
        return decoder.skipGetInt(HBSPackage.DecodeSkip.INT) +
                decoder.skipGetInt(
                HBSPackage.DecodeSkip.INT,
                HBSPackage.DecodeSkip.INT,
                HBSPackage.DecodeSkip.STRING);
    }
}
