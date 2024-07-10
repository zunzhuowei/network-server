package com.hbsoo.server.action.server;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 内部服务器登录接口
 * Created by zun.wei on 2024/6/6.
 */
@InsideServerMessageHandler(MessageType.Inside.LOGIN)
public class InsideServerLoginAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerLoginAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        int serverId = decoder.readInt();//登录进来的服务器id
        String serverTypeStr = decoder.readStr();//登录进来的服务器类型
        int index = decoder.readInt();//客户端编号
        int id = decoder.readInt();//当前服务器id
        String loginServerTypeStr = decoder.readStr();//当前服务器类型
        //把id放入属性中
        ctx.channel().attr(AttributeKeyConstants.idAttr).set((long) serverId);
        ctx.channel().attr(AttributeKeyConstants.isInnerClientAttr).set(true);
        NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.LOGIN)
                .writeInt(id)//当前服务器id
                .writeStr(loginServerTypeStr)//当前服务器类型
                .writeInt(index)//客户端编号
                .sendTcpTo(ctx.channel());
        logger.info("接收到内部服务器登录消息：InsideServerLoginAction login success,serverType[{}],id[{}],index[{}]", serverTypeStr, serverId, index);

        // 内网服务器登录，将已登录的用户session同步给登录服务器
        if (index == 0) {
            Map<Long, UserSession> clients = outsideUserSessionManager.getClients();
            clients.forEach((userId, userSession) -> {
                Gson gson = new Gson();
                // 登录服务器
                NetworkPacket.Builder.withDefaultHeader()
                        .msgType(MessageType.Inside.LOGIN_SYNC)
                        .writeLong(userId)//登录用户id
                        .writeInt(userSession.getBelongServer().getId()) //登录所属服务器id
                        .writeStr(userSession.getBelongServer().getHost())
                        .writeInt(userSession.getBelongServer().getPort())
                        .writeStr(userSession.getBelongServer().getType())
                        .writeStr(gson.toJson(userSession.getPermissions()))
                        .sendTcpTo(ctx.channel());
                //必须用连接的客户端推送，否则可能对方客户端链接了但是服务端还未启动完成，导致无法收到消息
                //forward2InnerServerUseSender(builder, serverTypeStr, serverId);
            });
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        //服务器id + 客户端编号
        return decoder.skipGetInt(NetworkPacket.DecodeSkip.INT) +
                decoder.skipGetInt(
                        NetworkPacket.DecodeSkip.INT,
                        NetworkPacket.DecodeSkip.INT,
                        NetworkPacket.DecodeSkip.STRING);
    }
}
