package com.hbsoo.server.message;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by zun.wei on 2024/6/8.
 */
public class TextWebSocketPackage implements Serializable {

    private int msgType;

    private Map<String,Object> data;

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "TextWebSocketPackage{" +
                "msgType=" + msgType +
                ", data=" + data +
                '}';
    }
}
