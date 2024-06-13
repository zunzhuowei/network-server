package com.hbsoo.server.config;

import com.hbsoo.server.session.ServerType;

/**
 * Created by zun.wei on 2024/5/30.
 */
public class ServerInfo {

    /**
     * 服务器地址
     */
    private String host;

    /**
     * 服务器端口
     */
    private int port;

    /**
     * 服务器类型
     */
    private ServerType type;

    /**
     * 服务器id，唯一
     */
    private Integer id;

    /**
     * 链接该服务器的客户端数量
     */
    private Integer clientAmount;

    public Integer getClientAmount() {
        return clientAmount;
    }

    public void setClientAmount(Integer clientAmount) {
        this.clientAmount = clientAmount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
