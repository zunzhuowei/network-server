package com.hbsoo.server.config;


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
    private String type;

    /**
     * 服务器id，唯一
     */
    private Integer id;

    /**
     * 链接该服务器的客户端数量
     */
    private Integer clientAmount;

    /**
     * 服务器权重
     */
    private int weight;

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
