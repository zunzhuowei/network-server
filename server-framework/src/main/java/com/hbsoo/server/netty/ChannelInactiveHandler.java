package com.hbsoo.server.netty;

import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * 处理失效的链接
 * Created by zun.wei on 2024/6/17.
 */
public final class ChannelInactiveHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ProtocolDispatcher.ProtocolType protocolType;

    public ChannelInactiveHandler(ProtocolDispatcher.ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("ChannelInactiveHandler channelInactive protocolType:{}", protocolType.name());
        ctx.close();
        // 注销登录
        Long id = ctx.channel().attr(AttributeKeyConstants.idAttr).get();
        if (Objects.nonNull(id)) {
            Boolean isInnerClient = ctx.channel().attr(AttributeKeyConstants.isInnerClientAttr).get();
            if (Objects.isNull(isInnerClient)) {
                OutsideUserSessionManager manager = SpringBeanFactory.getBean(OutsideUserSessionManager.class);
                //防止客户端重连的时候，旧的客户端被踢出时，把新的客户端踢出。
                UserSession userSession = manager.getUserSession(id);
                if (Objects.nonNull(userSession)) {
                    if (ctx.channel().id() == userSession.getChannel().id()) {
                        logger.debug("Outer channelInactive id = " + id);
                        manager.logoutAndSyncAllServer(id);
                    } else {
                        logger.debug("Outer channelInactive1 id = " + id);
                    }
                } else {
                    logger.debug("Outer channelInactive2 id = " + id);
                    manager.logoutAndSyncAllServer(id);
                }
            } else {
                logger.debug("Inner channelInactive id = " + id);
                InsideClientSessionManager.innerLogoutWithChannel(ctx.channel());
            }
        }
        ProtocolDispatcher.channels.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        if (cause instanceof IOException) {
            logger.warn("ChannelInactiveHandler exceptionCaught protocolType:{},cause:{}", protocolType.name(), cause.getMessage());
            return;
        }
        logger.warn("ChannelInactiveHandler exceptionCaught protocolType:{},cause:{}", protocolType.name(), cause);
    }
}
