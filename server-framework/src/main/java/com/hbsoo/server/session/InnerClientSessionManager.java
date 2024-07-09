package com.hbsoo.server.session;

import com.hbsoo.server.message.entity.ForwardMessage;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.SyncMessage;
import com.hbsoo.server.message.sender.ForwardMessageSender;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

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
    /**
     * 同步消息的map
     */
    public static Map<Long, SyncMessage> syncMsgMap = new ConcurrentHashMap<>();

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
     * @param serverId   目标服务器的ID，用于在服务器列表中定位目标服务器。
     * @param serverType 目标服务器的类型，用于获取相应类型服务器的连接列表。
     */
    public static void forwardMsg2ServerByTypeAndId(HBSPackage.Builder msgBuilder, int serverId, String serverType) {
        Channel channel = InnerSessionManager.getChannelByServerTypeAndId(serverId, serverType, () -> clientsMap);
        if (channel != null) {
            msgBuilder.sendTcpTo(channel);
        }
    }

    /**
     * 根据消息类型和键值向特定服务器发送消息。
     * 【注意】:方法会在消息体尾部追加消息ID。
     * 服务端接收到消息后，响应结果时使用channel返回，需要将消息ID也追加到消息体尾部。
     */
    public static HBSPackage.Decoder requestServerByTypeAndId(HBSPackage.Builder msgBuilder, int serverId, String serverType)
            throws InterruptedException, TimeoutException {
        return requestServer(msgBuilder, 5, (builder) -> forwardMsg2ServerByTypeAndId(builder, serverId, serverType));
    }

    /**
     * 请求服务器，并等待服务器响应返回值；
     * 【注意】:方法会在消息体【尾部追加消息ID】。
     * 服务端接收到消息后，响应结果时【必须使用channel】返回，需要【将消息ID也追加到消息体尾部】。
     * @param msgBuilder 消息内容
     * @param waitSeconds 等待相应结果时间秒数
     * @param forwardMsg2ServerFunction 消息发送函数
     * @return 服务器响应的内容或者null(等待返回值超时)
     */
    public static HBSPackage.Decoder requestServer(HBSPackage.Builder msgBuilder, int waitSeconds, Consumer<HBSPackage.Builder> forwardMsg2ServerFunction)
            throws InterruptedException {
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        long generateId = snowflakeIdGenerator.generateId();
        try {
            msgBuilder.writeLong(generateId);
            forwardMsg2ServerFunction.accept(msgBuilder);
            SyncMessage syncMessage = syncMsgMap.computeIfAbsent(generateId, k -> new SyncMessage(new CountDownLatch(1)));
            CountDownLatch latch = syncMessage.getCountDownLatch();
            //阻塞等待结果
            boolean await = latch.await(waitSeconds, TimeUnit.SECONDS);
            if (await) {
                return syncMessage.getDecoder();
            }
            return null;
        } finally {
            syncMsgMap.remove(generateId);
        }
    }

    /**
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link InnerClientSessionManager#forwardMsg2ServerByTypeAndId}
     */
    public static void forwardMsg2ServerByTypeAndIdUseSender(HBSPackage.Builder msgBuilder, int serverId, String serverType) {
        ForwardMessageSender sender = SpringBeanFactory.getBean(ForwardMessageSender.class);
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        long msgId = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(msgId, msgBuilder, -1, -1,
                serverType, null);
        forwardMessage.setToServerId(serverId);
        sender.send(forwardMessage);
    }

    /**
     * 根据消息类型和键值向特定服务器发送消息。
     * 使用Builder模式构建消息包，根据服务器类型和键值选择目标服务器，然后将消息发送到该服务器。
     * 注意：【消息不会发送给当前服务器】。
     *
     * @param msgBuilder 消息包的Builder对象，用于构建消息包。
     * @param serverType 服务器类型，用于确定目标服务器群组。
     *                   注意：如果与当前服务器类型相同，则发送到当前服务器之外的服务器。
     * @param key        根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     *                   抛出异常：如果key为null，则抛出RuntimeException。
     */
    public static void forwardMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        Channel channel = InnerSessionManager.getChannelByTypeAndKey(serverType, key, () -> clientsMap);
        if (channel != null) {
            msgBuilder.sendTcpTo(channel);
        }
    }
    /**
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link InnerClientSessionManager#forwardMsg2ServerByTypeAndKey}
     */
    public static void forwardMsg2ServerByTypeAndKeyUseSender(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        ForwardMessageSender sender = SpringBeanFactory.getBean(ForwardMessageSender.class);
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        long msgId = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(msgId, msgBuilder, -1, -1,
                serverType, key);
        sender.send(forwardMessage);
    }

    /**
     * 根据消息类型和键值向指定类型的可用的服务器发送消息.
     */
    public static void forwardMsg2AvailableServerByTypeAndKey(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        Channel channel = InnerSessionManager.getAvailableChannelByTypeAndKey(serverType, key, () -> clientsMap);
        if (channel != null) {
            msgBuilder.sendTcpTo(channel);
        }
    }

    /**
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link InnerClientSessionManager#forwardMsg2AvailableServerByTypeAndKey}
     */
    public static void forwardMsg2AvailableServerByTypeAndKeyUseSender(HBSPackage.Builder msgBuilder, String serverType, Object key) {
        ForwardMessageSender sender = SpringBeanFactory.getBean(ForwardMessageSender.class);
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        long msgId = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(msgId, msgBuilder, -1, -1,
                serverType, key);
        forwardMessage.setUseAvailableServer(true);
        sender.send(forwardMessage);
    }

    /**
     * 根据键值向所有服务器发送消息。
     * 使用Builder模式构建消息包，遍历所有服务器，将消息发送到每个服务器。
     * 注意：【消息不会发送给当前服务器】。
     *
     * @param msgBuilder 消息包的Builder对象，用于构建消息包。
     * @param key        根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     *                   抛出异常：如果key为null，则抛出RuntimeException。
     */
    public static void forwardMsg2AllServerByKey(HBSPackage.Builder msgBuilder, Object key) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        clientsMap.forEach((serverType, serverTypeMap) -> {
            Channel channel = InnerSessionManager.getChannelByTypeAndKey(serverType, key, () -> clientsMap);
            if (channel != null) {
                msgBuilder.sendTcpTo(channel);
            }
        });
    }
    /**
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link InnerClientSessionManager#forwardMsg2AllServerByKey}
     */
    public static void forwardMsg2AllServerByKeyUseSender(HBSPackage.Builder msgBuilder, Object key) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        ForwardMessageSender sender = SpringBeanFactory.getBean(ForwardMessageSender.class);
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        clientsMap.forEach((serverType, serverTypeMap) -> {
            long msgId = snowflakeIdGenerator.generateId();
            ForwardMessage forwardMessage = new ForwardMessage(msgId, msgBuilder, -1, -1,
                    serverType, key);
            sender.send(forwardMessage);
        });
    }

    /**
     * 根据服务器类型发送给该类型的所有服务器
     */
    public static void forwardMsg2ServerByTypeAll(HBSPackage.Builder msgBuilder, String serverType) {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> map = clientsMap.get(serverType);
        for (ConcurrentHashMap<Integer, Channel> value : map.values()) {
            Channel channel = value.get(0);
            if (channel != null) {
                msgBuilder.sendTcpTo(channel);
            }
        }
    }

    /**
     * 根据服务器类型发送给该类型的所有服务器
     * 使用sender发送，保证发送失败时候重发消息；
     * {@link InnerClientSessionManager#forwardMsg2ServerByTypeAll}
     */
    public static void forwardMsg2AllServerByTypeUseSender(HBSPackage.Builder msgBuilder, String serverType) {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> map = clientsMap.get(serverType);
        for (Integer serverId : map.keySet()) {
            ForwardMessageSender sender = SpringBeanFactory.getBean(ForwardMessageSender.class);
            SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
            long msgId = snowflakeIdGenerator.generateId();
            ForwardMessage forwardMessage = new ForwardMessage(msgId, msgBuilder, -1, -1,
                    serverType, null);
            forwardMessage.setToServerId(serverId);
            sender.send(forwardMessage);
        }
    }

    /**
     * 根据配置的服务器类型和权重，向特定服务器发送消息。如果未配置权重值，则随机选择一个服务器发送消息。
     * 注意：【消息不会发送给当前服务器】。
     *
     * @param msgBuilder 消息构建器，用于构建待发送的消息包。
     * @param serverType 服务器类型，用于定位服务器集群。
     * @throws RuntimeException 如果键值为null，则抛出运行时异常。
     */
    public static void forwardMsg2ServerByTypeUseWeight(HBSPackage.Builder msgBuilder, String serverType) {
        Channel channel = InnerSessionManager.getChannelByTypeUseWeight(serverType, () -> clientsMap);
        if (channel != null) {
            msgBuilder.sendTcpTo(channel);
        }
    }

    /**
     * 根据服务器类型和键值获取对应的服务器连接通道。
     * @param serverType 服务器类型
     * @param forwardKey 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * @return 服务器连接通道
     */
    public static Channel getChannelByTypeAndKey(String serverType, Object forwardKey) {
        return InnerSessionManager.getChannelByTypeAndKey(serverType, forwardKey, () -> clientsMap);
    }

    /**
     * 根据服务器类型和键值获取对应的服务器连接通道。
     * key计算出的服务器不可用时，重新选举相同类型的可用服务器链接作为备选；
     * @param serverType 服务器类型
     * @param forwardKey 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * @return 服务器连接通道
     */
    public static Channel getAvailableChannelByTypeAndKey(String serverType, Object forwardKey) {
        return InnerSessionManager.getAvailableChannelByTypeAndKey(serverType, forwardKey, () -> clientsMap);
    }

    /**
     * 根据服务器ID和类型获取对应的服务器连接通道。
     * 注意：获取的channel是随机选取的，不保证每次获取的channel都是同一个。
     * @param serverId 服务器ID
     * @param serverType 服务器类型
     * @return 服务器连接通道
     */
    public static Channel getChannelByServerTypeAndId(int serverId, String serverType) {
        return InnerSessionManager.getChannelByServerTypeAndId(serverId, serverType, () -> clientsMap);
    }
}
