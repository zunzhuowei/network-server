package com.hbsoo.server.config;

import com.hbsoo.server.session.ServerType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Created by zun.wei on 2024/5/30.
 */
@ConfigurationProperties(prefix = "hbsoo.server")
public class ServerInfoProperties {

    private List<ServerInfo> innerServers;
    private Map<String, Object> port;
    private Integer id;
    private ServerType type;


    public ServerType getType() {
        return type;
    }

    public void setType(ServerType type) {
        this.type = type;
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

    public Map<String, Object> getPort() {
        return port;
    }

    public void setPort(Map<String, Object> port) {
        this.port = port;
    }
}
