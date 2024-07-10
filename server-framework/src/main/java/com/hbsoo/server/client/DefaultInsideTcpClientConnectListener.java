package com.hbsoo.server.client;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zun.wei on 2024/6/27.
 */
public class DefaultInsideTcpClientConnectListener implements InsideTcpClientConnectListener {

    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void onConnectSuccess(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index) {
        // 登录消息
        byte[] aPackage = NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.LOGIN)
                .writeInt(fromServerInfo.getId())//当前服务器的ID
                .writeStr(fromServerInfo.getType())//当前服务器的类型
                .writeInt(index)//客户端编号
                .writeInt(toServerInfo.getId())//登录服务器的ID
                .writeStr(toServerInfo.getType())//登录服务器的类型
                .buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(aPackage);
        channelFuture.channel().writeAndFlush(buf);
    }

    @Override
    public void onConnectFail(ChannelFuture channelFuture, ServerInfo fromServerInfo, ServerInfo toServerInfo, int index) {
        // 清除登陆在断线的服务器中的所有用户
        if (index == 0) {
            outsideUserSessionManager.logoutWithBelongServer(toServerInfo.getType(), toServerInfo.getId());
        }
    }
}
