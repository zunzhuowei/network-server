package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.session.OuterSessionManager;
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
@InnerServerMessageHandler(HBSMessageType.Inner.LOGIN)
public class InnerServerLoginAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InnerServerLoginAction.class);
    @Autowired
    private OuterSessionManager outerSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        int serverId = decoder.readInt();//登录进来的服务器id
        String serverTypeStr = decoder.readStr();//登录进来的服务器类型
        int index = decoder.readInt();//客户端编号
        int id = decoder.readInt();//当前服务器id
        String loginServerTypeStr = decoder.readStr();//当前服务器类型
        //把id放入属性中
        ctx.channel().attr(AttributeKeyConstants.idAttr).set((long) serverId);
        ctx.channel().attr(AttributeKeyConstants.isInnerClientAttr).set(true);
        //InnerServerSessionManager.innerLogin(ServerType.valueOf(serverTypeStr), serverId, ctx.channel(), index);
        HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Inner.LOGIN)
                .writeInt(id)//当前服务器id
                .writeStr(loginServerTypeStr)//当前服务器类型
                .writeInt(index)//客户端编号
                .buildAndSendBytesTo(ctx.channel());
        logger.info("接收到内部服务器登录消息：InnerServerLoginAction login success,serverType[{}],id[{}],index[{}]", serverTypeStr, serverId, index);

        // 内网服务器登录，将已登录的用户session同步给登录服务器
        if (index == 0) {
            Map<Long, UserSession> clients = outerSessionManager.getClients();
            clients.forEach((userId, userSession) -> {
                // 登录服务器
                HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.Inner.LOGIN_SYNC)
                        .writeLong(userId)//登录用户id
                        //.writeStr(userSession.getName())
                        //.writeStr(userSession.getToken())
                        .writeInt(userSession.getBelongServer().getId()) //登录所属服务器id
                        .writeStr(userSession.getBelongServer().getHost())
                        .writeInt(userSession.getBelongServer().getPort())
                        .writeStr(userSession.getBelongServer().getType());
                forward2InnerServer(builder, serverTypeStr, serverId);
            });
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        //服务器id + 客户端编号
        return decoder.skipGetInt(HBSPackage.DecodeSkip.INT) +
                decoder.skipGetInt(
                        HBSPackage.DecodeSkip.INT,
                        HBSPackage.DecodeSkip.INT,
                        HBSPackage.DecodeSkip.STRING);
    }
}
