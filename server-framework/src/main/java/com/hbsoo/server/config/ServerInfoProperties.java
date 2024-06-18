package com.hbsoo.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/5/30.
 */
@ConfigurationProperties(prefix = "hbsoo.server")
public final class ServerInfoProperties {

    private List<ServerInfo> innerServers;
    private Map<String, Object> outerServer;
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

    public List<ServerInfo> getInnerServers() {
        return innerServers;
    }

    public void setInnerServers(List<ServerInfo> innerServers) {
        this.innerServers = innerServers;
    }

    public Map<String, Object> getOuterServer() {
        return outerServer;
    }

    public void setOuterServer(Map<String, Object> outerServer) {
        this.outerServer = outerServer;
    }
}
