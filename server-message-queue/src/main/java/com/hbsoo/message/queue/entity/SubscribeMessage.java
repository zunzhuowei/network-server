package com.hbsoo.message.queue.entity;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.entity.NetworkPacketEntity;
import com.hbsoo.server.message.entity.NetworkPacket;

/**
 * Created by zun.wei on 2024/6/26.
 */
public final class SubscribeMessage implements NetworkPacketEntity<SubscribeMessage> {

    /**
     * 订阅主题
     */
    private String topic;
    /**
     * 服务器类型
     */
    private String serverType;
    /**
     * 服务器id
     */
    private int serverId;

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }


    @Override
    public void serializable(NetworkPacket.Builder builder) {
        ServerInfo serverInfo = NowServer.getServerInfo();
        builder.writeStr(serverInfo.getType())
                .writeInt(serverInfo.getId())
                .writeString(this.topic);
    }

    @Override
    public SubscribeMessage deserialize(NetworkPacket.Decoder decoder) {
        String serverType = decoder.readStr();
        int serverId = decoder.readInt();
        String topic = decoder.readStr();
        try {
            this.setTopic(topic);
            this.setServerType(serverType);
            this.setServerId(serverId);
            return this;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
