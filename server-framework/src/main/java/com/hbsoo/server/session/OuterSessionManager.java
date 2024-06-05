package com.hbsoo.server.session;

import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/5/31.
 */
public final class OuterSessionManager {

    static Map<Long, Channel> clients = new ConcurrentHashMap<>();
    private Integer serverId;
    private ServerType serverType;

    public OuterSessionManager(Integer serverId, ServerType serverType) {
        this.serverId = serverId;
        this.serverType = serverType;
    }

    public void login(Long id, Channel channel) {
        clients.put(id, channel);
    }

    public void logout(Long id) {
        clients.remove(id);
    }

    public void sendMsg2ServerByTypeWithServerSide(HBSPackage.Builder msgBuilder, ServerType serverType) {
        InnerServerSessionManager.sendMsg2ServerByType(msgBuilder, serverType);
    }

    public void sendMsg2ServerByTypeWithClientSide(HBSPackage.Builder msgBuilder, ServerType serverType) {
        InnerClientSessionManager.sendMsg2ServerByType(msgBuilder, serverType);
    }

    public void sendMsg2ServerByTypeAndKeyWithServerSide(HBSPackage.Builder msgBuilder, ServerType serverType, String key) {
        InnerServerSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }

    public void sendMsg2ServerByTypeAndKeyWithClientSide(HBSPackage.Builder msgBuilder, ServerType serverType, String key) {
        InnerClientSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key);
    }
    public void sendMsg2AllServerWithServerSide(HBSPackage.Builder msgBuilder) {
        InnerServerSessionManager.sendMsg2AllServer(msgBuilder);
    }
    public void sendMsg2AllServerWithClientSide(HBSPackage.Builder msgBuilder) {
        InnerClientSessionManager.sendMsg2AllServer(msgBuilder);
    }

    private void syncSession2OtherServerWithServerSide(ServerType serverType, Long id, int msgType) {
        final HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                //.writeInt(HBSMessageType.OuterMessageType.LOGIN_SYNC)
                .writeInt(msgType)
                .writeLong(id)
                .writeStr(this.serverType.name())
                .writeInt(this.serverId);
        InnerServerSessionManager.sendMsg2ServerByType(builder, serverType);
    }

    private void syncSession2OtherServerWithClientSide(ServerType serverType, Long id, int msgType) {
        final HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                //.writeInt(HBSMessageType.OuterMessageType.LOGIN_SYNC)
                .writeInt(msgType)
                .writeLong(id)
                .writeStr(this.serverType.name())
                .writeInt(this.serverId);
        InnerClientSessionManager.sendMsg2ServerByType(builder, serverType);
    }

}
