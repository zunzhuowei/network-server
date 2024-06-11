package com.hbsoo.server.session;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内网客户端，管道存储集合
 * Created by zun.wei on 2024/6/5.
 */
public final class InnerClientSessionManager {

    // 使用slf4j作为日志记录工具
    private static final Logger logger = LoggerFactory.getLogger(InnerClientSessionManager.class);
    /*
    {
        {gateway:{1000:{0:channel,1:channel},2000:{0:channel,1:channel}}} ,
        {room:{1000:{0:channel,1:channel},2000:{0:channel,1:channel}}},
        {hall:{1000:{0:channel,1:channel},2000:{0:channel,1:channel}}}
    }
     */
    //关于key的解释：1.serverType:服务器类型 2.serverId:服务器id 3.链接服务器的客户端编号
    public static Map<ServerType, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = new ConcurrentHashMap<>();

    static {
        // 初始化数据
        for (ServerType serverType : ServerType.values()) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> innerMap = new ConcurrentHashMap<>();
            clientsMap.put(serverType, innerMap);
        }

    }

    public static void innerLogin(ServerType serverType, Integer serverId, Channel channel, int index) {
        InnerSessionManager.innerLogin(serverType, serverId, channel, index, () -> clientsMap);
    }
    public static void innerLogout(ServerType serverType, Integer serverId) {
        InnerSessionManager.innerLogout(serverType, serverId, () -> clientsMap);
    }

    public static void innerLogoutWithChannel(Channel channel) {
        InnerSessionManager.innerLogoutWithChannel(channel, () -> clientsMap);
    }

    /**
     * 根据服务器ID和服务器类型向指定服务器发送消息。
     * 此方法首先构建消息包，然后尝试找到目标服务器的连接通道，最后通过通道发送消息。
     * 如果无法找到对应的服务器连接或发送消息失败，将打印错误信息。
     *
     * @param msgBuilder 消息包的构建器，用于构建待发送的消息包。
     * @param serverId 目标服务器的ID，用于在服务器列表中定位目标服务器。
     * @param serverType 目标服务器的类型，用于获取相应类型服务器的连接列表。
     */
    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId, ServerType serverType) {
        InnerSessionManager.sendMsg2ServerByServerId(msgBuilder, serverId, serverType, () -> clientsMap);
    }

    /**
     * 根据消息类型和键值向特定服务器发送消息。
     * 使用Builder模式构建消息包，根据服务器类型和键值选择目标服务器，然后将消息发送到该服务器。
     *
     * @param msgBuilder 消息包的Builder对象，用于构建消息包。
     * @param serverType 服务器类型，用于确定目标服务器群组。
     *                   注意：如果与当前服务器类型相同，则发送到当前服务器之外的服务器。
     * @param key 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * 抛出异常：如果key为null，则抛出RuntimeException。
     */
    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, ServerType serverType, Object key) {
        InnerSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key, () -> clientsMap);
    }
}
