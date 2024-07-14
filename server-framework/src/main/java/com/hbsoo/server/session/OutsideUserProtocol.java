package com.hbsoo.server.session;

/**
 * Created by zun.wei on 2024/6/12.
 */
public enum OutsideUserProtocol {

    BINARY_WEBSOCKET((byte) 2),
    TEXT_WEBSOCKET((byte) 3),
    HTTP((byte) 4),
    TCP((byte) 0),
    UDP((byte) 1);

    //byte tcp = 0, udp = 1, binary_websocket = 2,text_websocket = 3, http = 4;
    public byte protocolType;

    OutsideUserProtocol(byte protocolType) {
        this.protocolType = protocolType;
    }

    public static OutsideUserProtocol getProtocol(byte protocolType) {
        for (OutsideUserProtocol protocol : OutsideUserProtocol.values()) {
            if (protocol.protocolType == protocolType) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("protocolType is not support:" + protocolType);
    }

}
