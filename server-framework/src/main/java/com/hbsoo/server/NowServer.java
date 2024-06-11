package com.hbsoo.server;

import com.hbsoo.server.config.ServerInfo;

/**
 * 当前服务器信息
 * Created by zun.wei on 2024/6/11.
 */
public final class NowServer {

    private static ServerInfo SERVER_INFO;

    public static ServerInfo getServerInfo() {
        return SERVER_INFO;
    }
    public static void setServerInfo(ServerInfo serverInfo) {
        SERVER_INFO = serverInfo;
    }
}
