package com.hbsoo.server.client.outer;

import io.netty.channel.ChannelHandlerContext;

/**
 * 外部用户登录认证器，
 * 业务端只需要继承该类，实现认证逻辑即可以将用户信息保存到集群session中
 * 认证成功后，将用户信息保存到session中
 */
interface OuterUserLoginAuthenticator<T> {

    /**
     * 认证
     * @param t 消息类型，HBSPackage.Decoder (tcp,websocket,udp)或者HttpPackage(http)
     * @return 用户id,如果未空表示认证未通过
     */
    Long authentication(ChannelHandlerContext ctx, T t);

    /**
     * 登出回调
     * @param id 用户id
     */
    void logoutCallback(Long id);

}
