package com.hbsoo.gateway.action.http;

import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * 仅 regression profile：需 JWT 且 permission 含 ADMIN。
 */
@Profile("regression")
@PermissionAuth(permission = {Permission.ADMIN})
@OutsideMessageHandler(value = 0, uri = "/regression/protected", protocol = Protocol.HTTP)
public class RegressionJwtProtectedAction extends HttpServerMessageDispatcher {

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        responseJson(httpPacket, Map.of("status", "ok", "message", "jwt authorized"));
    }
}
