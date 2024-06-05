package com.hbsoo.server.message.server.inner;

import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.session.InnerServerSessionManager;
import com.hbsoo.server.session.ServerType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerTcpServerMessageHandler implements InnerServerMessageHandler<ByteBuf> {

    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] received = new byte[msg.readableBytes()];
        msg.readBytes(received);
        final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
        final int msgType = decoder.readInt();
        // 登录
        if (msgType == HBSMessageType.InnerMessageType.LOGIN) {
            final int serverId = decoder.readInt();
            final String serverTypeStr = decoder.readStr();
            final int id = decoder.readInt();
            InnerServerSessionManager.innerLogin(ServerType.valueOf(serverTypeStr), serverId, ctx.channel());
            //decoder.resetBodyReadOffset();
            ctx.channel().writeAndFlush(
                    HBSPackage.Builder.withDefaultHeader()
                            .writeInt(HBSMessageType.InnerMessageType.LOGIN)
                            .writeInt(id)
                            .buildPackage()
            );
            return;
        }
        onMessage(ctx, decoder);
    }

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

}
