package com.hbsoo.server;

import com.hbsoo.server.config.ServerInfo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 当前服务器信息
 * Created by zun.wei on 2024/6/11.
 */
public final class NowServer {

    private static ServerInfo SERVER_INFO;
    private final static Set<String> serverTypes = new CopyOnWriteArraySet<>();
    private final static List<ServerInfo> innerServers = new CopyOnWriteArrayList<>();

    public static ServerInfo getServerInfo() {
        return SERVER_INFO;
    }
    public static void setServerInfo(ServerInfo serverInfo) {
        SERVER_INFO = serverInfo;
    }

    public static void addServerType(String serverType) {
        serverTypes.add(serverType);
    }
    public static boolean isServerType(String serverType) {
        return serverTypes.contains(serverType);
    }
    public static void removeServerType(String serverType) {
        serverTypes.remove(serverType);
    }
    public static Set<String> getServerTypes() {
        return serverTypes;
    }

    public static void addInnerServer(ServerInfo serverInfo) {
        innerServers.add(serverInfo);
    }
    public static List<ServerInfo> getInnerServers() {
        return innerServers;
    }
}
