package com.hbsoo.message.queue.entity;

import java.util.Objects;

/**
 * Created by zun.wei on 2024/7/1.
 */
public final class CallbackMessage {

    private final String objJson;
    private final String callbackServerType;
    private final int callbackServerId;
    private final String mqServerType;
    private final int mqServerId;
    private final Long msgId;
    // 服务器执行结果
    private final boolean callbackResult;
    private final String topic;


    public String getTopic() {
        return topic;
    }

    public String getObjJson() {
        return objJson;
    }

    public String getCallbackServerType() {
        return callbackServerType;
    }

    public boolean getCallbackResult() {
        return callbackResult;
    }

    public int getCallbackServerId() {
        return callbackServerId;
    }

    public Long getMsgId() {
        return msgId;
    }

    public String getMqServerType() {
        return mqServerType;
    }

    public int getMqServerId() {
        return mqServerId;
    }

    public CallbackMessage(Long msgId, String topic, String objJson,
                           String callbackServerType, int callbackServerId,
                           String mqServerType, int mqServerId,
                           boolean callbackResult) {
        this.topic = topic;
        this.mqServerType = mqServerType;
        this.mqServerId = mqServerId;
        this.msgId = msgId;
        this.objJson = objJson;
        this.callbackServerType = callbackServerType;
        this.callbackResult = callbackResult;
        this.callbackServerId = callbackServerId;
    }

    @Override
    public String toString() {
        return "CallbackMessage{" +
                "objJson='" + objJson + '\'' +
                ", callbackServerType='" + callbackServerType + '\'' +
                ", callbackServerId=" + callbackServerId +
                ", msgId=" + msgId +
                ", callbackResult=" + callbackResult +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackMessage that = (CallbackMessage) o;
        return callbackServerId == that.callbackServerId && callbackResult == that.callbackResult
                && Objects.equals(objJson, that.objJson) && Objects.equals(callbackServerType, that.callbackServerType)
                && Objects.equals(msgId, that.msgId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objJson, callbackServerType, callbackServerId, msgId, callbackResult);
    }
}
