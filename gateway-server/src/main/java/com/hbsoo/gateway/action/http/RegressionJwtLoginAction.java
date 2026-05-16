package com.hbsoo.gateway.action.http;

import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.permisson.utils.JwtUtils;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * 仅 regression profile：签发 JWT，供端到端回归使用。
 */
@Profile("regression")
@PermissionAuth(permission = {})
@OutsideMessageHandler(value = 0, uri = "/regression/login", protocol = Protocol.HTTP)
public class RegressionJwtLoginAction extends HttpServerMessageDispatcher {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        String token = jwtUtils.generateToken("regression-user",
                Map.of("permission", "ADMIN,USER"));
        responseJson(httpPacket, Map.of(
                "token", token,
                "header", "Authentication"
        ));
    }
}
