package com.hbsoo.server.action.mqtt;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/7/23.
 */
public final class SubscribeInfo {

    private String fromServerType;
    private int fromServerId;
    private String userChannelId;

    public String getFromServerType() {
        return fromServerType;
    }

    public void setFromServerType(String fromServerType) {
        this.fromServerType = fromServerType;
    }

    public int getFromServerId() {
        return fromServerId;
    }

    public void setFromServerId(int fromServerId) {
        this.fromServerId = fromServerId;
    }

    public String getUserChannelId() {
        return userChannelId;
    }

    public void setUserChannelId(String userChannelId) {
        this.userChannelId = userChannelId;
    }

    @Override
    public String toString() {
        return "SubscribeInfo{" +
                "fromServerType='" + fromServerType + '\'' +
                ", fromServerId=" + fromServerId +
                ", userChannelId='" + userChannelId + '\'' +
                '}';
    }

    public static SubscribeInfo build(String fromServerType, int fromServerId, String userChannelId) {
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setFromServerType(fromServerType);
        subscribeInfo.setFromServerId(fromServerId);
        subscribeInfo.setUserChannelId(userChannelId);
        return subscribeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscribeInfo that = (SubscribeInfo) o;
        return fromServerId == that.fromServerId && Objects.equals(fromServerType, that.fromServerType)
                && Objects.equals(userChannelId, that.userChannelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromServerType, fromServerId, userChannelId);
    }
}
