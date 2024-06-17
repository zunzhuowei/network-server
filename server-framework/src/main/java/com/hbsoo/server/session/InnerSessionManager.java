package com.hbsoo.server.session;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/11.
 */
class InnerSessionManager {

    // 使用slf4j作为日志记录工具
    private static final Logger logger = LoggerFactory.getLogger(InnerSessionManager.class);

    public static void innerLogin(String serverType, Integer serverId, Channel channel, int index,
                                  Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        if (clients.containsKey(index)) {
            clients.get(index).close();
            clients.remove(index);
        }
        clients.put(index, channel);
    }
    public static void innerLogout(String serverType, Integer serverId,
                                   Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        clients.values().forEach(ChannelOutboundInvoker::close);
        clients.remove(serverId);
    }

    /**
     * 根据channel退出登录
     */
    public static void innerLogoutWithChannel(Channel channel,
                                              Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        clientsMap.values().forEach(servers -> {
            servers.values().forEach(server -> {
                server.forEach((index, ch) -> {
                    if (ch == channel) {
                        ch.close();
                        server.remove(index);
                    }
                });
            });
        });
    }

    /**
     * 发送消息到指定服务器
     *
     * @param msgBuilder 消息
     * @param serverId   服务器id
     * @param serverType 服务器类型
     */
    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId, String serverType,
                                                Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.get(serverType);
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        if (clients.isEmpty()) {
            logger.error("sendMsg2ServerByServerId error 服务器未登录:{}", serverId);
            return;
        }
        final Random random = new Random();
        int key = random.nextInt();
        int hash = Integer.hashCode(key);
        selectChannel2Msg(msgBuilder, hash, clients);
    }

    /**
     * 根据类型和键值向特定服务器发送消息。
     * 使用哈希算法和服务器列表的索引来决定消息发送到哪个服务器。
     * 此方法确保了消息的路由逻辑，使得消息能够根据服务器类型和键值被正确地路由到相应的服务器。
     * 注意：【消息不会发送给当前服务器】。
     *
     * @param msgBuilder 消息构建器，用于构建待发送的消息包。
     * @param serverType 服务器类型，用于定位服务器集群。
     * @param key 键值，用于计算消息应该发送到哪个服务器和选择哪个客户端发送
     * @throws RuntimeException 如果键值为null，则抛出运行时异常。
     */
    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, String serverType, Object key,
                                                  Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        if (key == null) {
            msgBuilder = null;
            throw new RuntimeException("key is null");
        }
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        //判断使用哪个服务器
        //ServerInfo serverInfo = NowServer.getServerInfo();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.get(serverType);
        //int typeSize = serverInfo.getType() == serverType ? serverTypeMap.size() - 1 : serverTypeMap.size();
        int typeSize = serverTypeMap.size();
        if (typeSize < 1) {
            msgBuilder = null;
            //throw new RuntimeException("typeSize < 1 : " + serverType.name());
            logger.warn("sendMsg2ServerByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return;
        }
        int hash = key.hashCode();
        int serverIndex = Math.abs(hash) % typeSize;
        final List<Integer> serverIds = serverTypeMap.keySet().stream()
                //.filter(k -> !k.equals(serverInfo.getId()))//排除当前服务器
                .sorted().collect(Collectors.toList());
        Integer serverId = serverIds.get(serverIndex);
        //根据key的hash值判断使用哪个客户端
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        selectChannel2Msg(msgBuilder, hash, clients);
    }

    /**
     * 选择一个客户端发送消息
     * @param msgBuilder 消息
     * @param hash 分配的hash值
     * @param clients 链接服务器的所有客户端
     */
    private static void selectChannel2Msg(HBSPackage.Builder msgBuilder, int hash, ConcurrentHashMap<Integer, Channel> clients) {
        if (clients.isEmpty()) {
            //throw new RuntimeException("clients.isEmpty()");
            logger.warn("clients.isEmpty()");
            return;
        }
        int index = Math.abs(hash) % clients.size();
        final List<Integer> clientIds = clients.keySet().stream().sorted().collect(Collectors.toList());
        Integer clientId = clientIds.get(index);
        final Channel channel = clients.get(clientId);
        if (channel == null) {
            msgBuilder = null;
            logger.warn("channel is null");
            return;
        }
        try {
            final byte[] msg = msgBuilder.buildPackage();
            ByteBuf buf = Unpooled.wrappedBuffer(msg);
            channel.writeAndFlush(buf).sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("sendMsg2ServerByTypeAndKey error:{}", e.getMessage());
        }
    }

    /**
     * 向所有服务器发送消息。
     * 此方法遍历服务器类型列表，并使用sendMsg2ServerByTypeAndKey方法发送消息给每个服务器。
     * 注意：【消息不会发送给当前服务器】。
     * @param msgBuilder 消息构建器，用于构建待发送的消息包。
     * @param key 键值，用于计算消息应该选择哪个客户端发送
     * @throws RuntimeException 如果键值为null，则抛出运行时异常。
     */
    public static void sendMsg2AllServerByKey(HBSPackage.Builder msgBuilder, Object key,
                                                  Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        clientsMap.forEach((serverType, serverTypeMap) -> {
            sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key, clientsMapSupplier);
        });
    }

    /**
     * 根据配置的服务器类型和权重，向特定服务器发送消息。如果未配置权重值，则随机选择一个服务器发送消息。
     * 注意：【消息不会发送给当前服务器】。
     *
     * @param msgBuilder 消息构建器，用于构建待发送的消息包。
     * @param serverType 服务器类型，用于定位服务器集群。
     * @throws RuntimeException 如果键值为null，则抛出运行时异常。
     */
    public static void sendMsg2ServerByTypeUseWeight(HBSPackage.Builder msgBuilder, String serverType,
                                                  Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.get(serverType);
        int typeSize = serverTypeMap.size();
        if (typeSize < 1) {
            msgBuilder = null;
            logger.warn("sendMsg2ServerByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return;
        }
        //当前服务器客户端登录到的其他服务器
        List<ServerInfo> innerServers = NowServer.getInnerServers().stream()
                .filter(serverInfo -> serverInfo.getType().equals(serverType))
                .collect(Collectors.toList());
        boolean useWeight = innerServers.stream().anyMatch(serverInfo -> serverInfo.getWeight() > 0);
        if (useWeight) {
            //根据权重比例随机选择一个服务器
            innerServers.sort(Comparator.comparingInt(ServerInfo::getWeight));
            int weightSum = innerServers.stream()
                    .map(ServerInfo::getWeight)
                    .mapToInt(Integer::intValue)
                    .sum();
            int randomWeight = new Random().nextInt(weightSum);
            int weightOffset = 0;
            for (int i = 0; i < innerServers.size(); i++) {
                ServerInfo serverInfo = innerServers.get(i);
                int weight = serverInfo.getWeight();
                if (randomWeight < weight + weightOffset) {
                    //根据服务器id获取客户端
                    ConcurrentHashMap<Integer, Channel> serverClients = serverTypeMap.get(serverInfo.getId());
                    if (serverClients == null || serverClients.isEmpty()) {
                        logger.warn("sendMsg2ServerByTypeAndKey error 服务器未登录:{}", serverInfo.getId());
                        return;
                    }
                    selectChannel2Msg(msgBuilder, randomWeight, serverClients);
                    break;
                }
                weightOffset += weight;
            }
            return;
        }
        logger.warn("sendMsg2ServerByTypeUseWeight useWeight=false 当前服务器类型未配置权重值:{}，将随机分配一个服务器", serverType);
        //如果权重值未配置，再随机选择一个服务器
        final Random random = new Random();
        int key = random.nextInt();
        sendMsg2ServerByTypeAndKey(msgBuilder, serverType, key, clientsMapSupplier);
    }
}
