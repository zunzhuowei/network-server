package com.hbsoo.server.config;

import com.hbsoo.server.session.ServerType;

/**
 * Created by zun.wei on 2024/5/30.
 */
public class ServerInfo {

    private String host;

    private int port;

    private ServerType type;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerType getType() {
        return type;
    }

    public void setType(ServerType type) {
        this.type = type;
    }
}
