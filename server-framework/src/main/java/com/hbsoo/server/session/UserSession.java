package com.hbsoo.server.session;

import com.hbsoo.server.config.ServerInfo;
import io.netty.channel.Channel;

/**
 * Created by zun.wei on 2024/6/11.
 */
public class UserSession {

    private Long id;
    private String name;
    private String token;
    private ServerInfo belongServer;
    /**
     * 登录的channel；如果由内网登录同步得session，则为空
     */
    private Channel channel;
    private String udpHost;
    private int udpPort;
    private boolean isUdp;

    public String getUdpHost() {
        return udpHost;
    }

    public void setUdpHost(String udpHost) {
        this.udpHost = udpHost;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public boolean isUdp() {
        return isUdp;
    }

    public void setUdp(boolean udp) {
        isUdp = udp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ServerInfo getBelongServer() {
        return belongServer;
    }

    public void setBelongServer(ServerInfo belongServer) {
        this.belongServer = belongServer;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", token='" + token + '\'' +
                ", belongServer=" + belongServer +
                ", channel=" + channel +
                '}';
    }
}
