package com.hbsoo.server.session;

import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.NetworkPacketEntity;
import io.netty.channel.Channel;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by zun.wei on 2024/6/11.
 */
public final class UserSession implements NetworkPacketEntity<UserSession> {

    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户所登录的服务器
     */
    private ServerInfo belongServer;
    /**
     * 登录的channel；如果由内网登录同步得session，则为空
     */
    private transient Channel channel;
    private String channelId;
    private String udpHost;
    private int udpPort;
    private boolean isUdp;
    /**
     * 用户有的权限
     */
    private final Set<String> permissions = new HashSet<>();


    public UserSession() { }
    public UserSession(String channelId) {
        this.channelId = channelId;
    }

    public void addPermission(String permission) {
        permissions.add(permission.toUpperCase());
    }

    public Set<String> getPermissions() {
        return permissions;
    }
    public String getChannelId() {
        return channelId;
    }
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
        this.channelId = channel.id().asLongText();
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "id=" + id +
                ", belongServer=" + belongServer +
                ", channel=" + channel +
                ", udpHost='" + udpHost + '\'' +
                ", udpPort=" + udpPort +
                ", isUdp=" + isUdp +
                '}';
    }

    @Override
    public void serializable(NetworkPacket.Builder builder) {
        builder.writeLong(this.id == null ? 0 : this.id)
                .writeStr(this.belongServer == null ? "" : this.belongServer.getHost())
                .writeInt(this.belongServer == null ? 0 : this.belongServer.getPort())
                .writeStr(this.belongServer == null ? "" : this.belongServer.getType())
                .writeInt(this.belongServer == null ? 0 : this.belongServer.getId())
                .writeInt(this.belongServer == null ? 0 : this.belongServer.getWeight())
                .writeInt(this.belongServer == null ? 0 : this.belongServer.getClientSize())
                .writeBoolean(this.isUdp)
                .writeStr(this.udpHost == null ? "" : this.udpHost)
                .writeInt(this.udpPort == 0 ? 0 : this.udpPort)
                .writeStr(this.channelId == null ? "" : this.channelId)
                .writeInt(this.permissions.size())
                .writeStr(this.permissions.toArray(new String[0]))
                ;
    }

    @Override
    public UserSession deserialize(NetworkPacket.Decoder decoder) {
       this.id = decoder.readLong();
        this.belongServer = new ServerInfo();
        this.belongServer.setHost(decoder.readStr());
        this.belongServer.setPort(decoder.readInt());
        this.belongServer.setType(decoder.readStr());
        this.belongServer.setId(decoder.readInt());
        this.belongServer.setWeight(decoder.readInt());
        this.belongServer.setClientSize(decoder.readInt());
        this.isUdp = decoder.readBoolean();
        this.udpHost = decoder.readStr();
        this.udpPort = decoder.readInt();
        this.channelId = decoder.readStr();
        int permissionSize = decoder.readInt();
        for (int i = 0; i < permissionSize; i++) {
            this.permissions.add(decoder.readStr());
        }
        return this;
    }
}
