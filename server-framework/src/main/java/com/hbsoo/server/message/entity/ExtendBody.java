package com.hbsoo.server.message.entity;

import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;


/**
 * Created by zun.wei on 2024/7/12.
 */
public final class ExtendBody implements NetworkPacketEntity<ExtendBody> {

    //消息id
    private long msgId;
    //协议类型：OutsideUserProtocol
    public byte protocolType;
    //来源服务器id
    private int fromServerId;
    //来源服务器类型
    private String fromServerType;
    // channel().id().asLongText();用户链接channelId或者是udp协议中的通信管道
    private String userChannelId;
    //是否登录
    private boolean isLogin;
    //用户id(登录后才有)
    private Long userId;
    //用户session(登录后才有)
    private UserSession userSession;
    //UDP协议，发送者ip
    private String senderHost;
    //UDP协议，发送者端口
    private int senderPort;
    //消息重试次数
    private int retryTimes;

    @Override
    public void serializable(NetworkPacket.Builder builder) {
        boolean writeRawBody = builder.isWriteRawBody();
        if (writeRawBody) {
            builder.writeExtendBodyMode();
        }
        builder.writeLong(this.msgId)
                .writeByte(this.protocolType)
                .writeInt(this.fromServerId)
                .writeStr(this.fromServerType)
                .writeStr(this.userChannelId)
                .writeBoolean(this.isLogin);
        if (isLogin) {
            builder.writeLong(this.userId).writeObj(this.userSession);
        }
        if (this.protocolType == OutsideUserProtocol.UDP.protocolType) {
            builder.writeStr(this.senderHost)
                    .writeInt(this.senderPort);
        }
        builder.writeInt(this.retryTimes);
        if (writeRawBody) {
            builder.writeRawBodyMode();
        }
    }

    @Override
    public ExtendBody deserialize(NetworkPacket.Decoder decoder) {
        boolean hasExtendBody = decoder.hasExtendBody();
        if (!hasExtendBody) {
            return null;
        }
        boolean isReadRawBody = decoder.isReadRawBody();
        if (isReadRawBody) {
            decoder.readExtendBodyMode();
        }
        this.msgId = decoder.readLong();
        this.protocolType = decoder.readByte();
        this.fromServerId = decoder.readInt();
        this.fromServerType = decoder.readStr();
        this.userChannelId = decoder.readStr();
        this.isLogin = decoder.readBoolean();
        if (isLogin) {
            this.userId = decoder.readLong();
            UserSession userSession = new UserSession();
            userSession.deserialize(decoder);
            this.userSession = userSession;
        }
        if (this.protocolType == OutsideUserProtocol.UDP.protocolType) {
            this.senderHost = decoder.readStr();
            this.senderPort = decoder.readInt();
        }
        this.retryTimes = decoder.readInt();
        if (isReadRawBody) {
            decoder.readRawBodyMode();
        }
        return this;
    }

    public OutsideUserProtocol getOutsideUserProtocol() {
        return OutsideUserProtocol.getProtocol(this.protocolType);
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public byte getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(byte protocolType) {
        this.protocolType = protocolType;
    }

    public int getFromServerId() {
        return fromServerId;
    }

    public void setFromServerId(int fromServerId) {
        this.fromServerId = fromServerId;
    }

    public String getFromServerType() {
        return fromServerType;
    }

    public void setFromServerType(String fromServerType) {
        this.fromServerType = fromServerType;
    }

    public String getUserChannelId() {
        return userChannelId;
    }

    public void setUserChannelId(String userChannelId) {
        this.userChannelId = userChannelId;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.isLogin = true;
        this.userId = userSession.getId();
        this.userSession = userSession;
    }

    public String getSenderHost() {
        return senderHost;
    }

    public void setSenderHost(String senderHost) {
        this.senderHost = senderHost;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    @Override
    public String toString() {
        return "ExtendBody{" +
                "msgId=" + msgId +
                ", protocolType=" + protocolType +
                ", fromServerId=" + fromServerId +
                ", fromServerType='" + fromServerType + '\'' +
                ", userChannelId='" + userChannelId + '\'' +
                ", isLogin=" + isLogin +
                ", userId=" + userId +
                ", userSession=" + userSession +
                ", senderHost='" + senderHost + '\'' +
                ", senderPort=" + senderPort +
                ", retryTimes=" + retryTimes +
                '}';
    }
}
