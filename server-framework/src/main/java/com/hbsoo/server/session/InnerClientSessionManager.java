package com.hbsoo.server.session;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存了当前服务器登录其他服务的channel;
 * 如服务器id：1000【当前服务器】,2000,3000；
 * 则这里保存的是1000客户端登录2000、3000服务器的channel
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
    //关于key的解释：1.serverType:服务器类型 2.serverId:服务器id 3.链接服务器的channel编号
    public static Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = new ConcurrentHashMap<>();


    public static void innerLogin(String serverType, Integer serverId, Channel channel, int index) {
        InnerSessionManager.innerLogin(serverType, serverId, channel, index, () -> clientsMap);
    }
    public static void innerLogout(String serverType, Integer serverId) {
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
    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId, String serverType) {
        InnerSessionManager.sendMsg2ServerByServerId(msgBuilder, serverId, serverType, () -> clientsMap);
    }

    /**
     * 根据消息类型和键值向特定服务器发送消息。
     * 使用Builder模式构建消息包，根据服务器类型和键值选择目标服务器，然后将消息发送到该服务器。
     * 注意：【消息不会发送给当前服务器】。
     * @param msgBuilder 消息包的Builder对象，用于构建消息包。
     * @param serverType 服务器类型，用于确定目标服务器群组。
     *                   注意：如果与当前服务器类型相同，则发送到当前服务器之外的服务器。
     * @param key 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * 抛出异常：如果key为null，则抛出RuntimeException。
     */
    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        InnerSessionManager.sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key, () -> clientsMap);
    }
    /**
     * 根据键值向所有服务器发送消息。
     * 使用Builder模式构建消息包，遍历所有服务器，将消息发送到每个服务器。
     * 注意：【消息不会发送给当前服务器】。
     * @param msgBuilder 消息包的Builder对象，用于构建消息包。
     * @param key 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * 抛出异常：如果key为null，则抛出RuntimeException。
     */
    public static void sendMsg2AllServerByKey(HBSPackage.Builder msgBuilder, Object key) {
        InnerSessionManager.sendMsg2AllServerByKey(msgBuilder, key, () -> clientsMap);
    }
}
