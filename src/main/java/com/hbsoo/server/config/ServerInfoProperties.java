package com.hbsoo.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/5/30.
 */
@ConfigurationProperties(prefix = "hbsoo.server")
public class ServerInfoProperties {

    private List<ServerInfo> innerClients;
    private Map<String, Object> port;

    public List<ServerInfo> getInnerClients() {
        return innerClients;
    }

    public void setInnerClients(List<ServerInfo> innerClients) {
        this.innerClients = innerClients;
    }

    public Map<String, Object> getPort() {
        return port;
    }

    public void setPort(Map<String, Object> port) {
        this.port = port;
    }
}
