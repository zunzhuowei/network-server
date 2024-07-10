package com.hbsoo.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/5/30.
 */
@ConfigurationProperties(prefix = "hbsoo.server")
public final class ServerInfoProperties {

    private List<ServerInfo> insideServers;
    private Map<String, Object> outsideServer;
    private Map<String, Object> threadPoolSize;
    private Integer id;

    public Map<String, Object> getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Map<String, Object> threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<ServerInfo> getInsideServers() {
        return insideServers;
    }

    public void setInsideServers(List<ServerInfo> insideServers) {
        this.insideServers = insideServers;
    }

    public Map<String, Object> getOutsideServer() {
        return outsideServer;
    }

    public void setOutsideServer(Map<String, Object> outsideServer) {
        this.outsideServer = outsideServer;
    }
}
