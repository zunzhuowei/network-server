package com.hbsoo.server.session;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
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
     *  根据serverId、ServerType获取channel
     * @param serverId 服务器id
     * @param serverType 服务器类型
     */
    public static Channel getChannelByServerTypeAndId(int serverId, String serverType,
                                                      Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        if (clients.isEmpty()) {
            logger.error("getChannelByServerId error 服务器未登录:{}", serverId);
            return null;
        }
        final Random random = new Random();
        int key = random.nextInt();
        int hash = Integer.hashCode(key);
        return selectChannelByHashKey(hash, clients);
    }

    /**
     * 根据服务器类型和key获取channel
     * @param serverType 服务器类型
     * @param key  根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     */
    public static Channel getChannelByTypeAndKey(String serverType, Object key,
                                                 Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        //判断使用哪个服务器
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        int typeSize = serverTypeMap.size();
        if (typeSize < 1) {
            logger.warn("getChannelByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return null;
        }
        int hash = key.hashCode();
        int serverIndex = Math.abs(hash) % typeSize;
        final List<Integer> serverIds = serverTypeMap.keySet().stream()
                //.filter(k -> !k.equals(serverInfo.getId()))//排除当前服务器
                .sorted().collect(Collectors.toList());
        Integer serverId = serverIds.get(serverIndex);
        //根据key的hash值判断使用哪个客户端
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        return selectChannelByHashKey(hash, clients);
    }

    /**
     * 选择一个客户端发送消息
     * @param hash 分配的hash值
     * @param clients 链接服务器的所有客户端
     */
    private static Channel selectChannelByHashKey(int hash, ConcurrentHashMap<Integer, Channel> clients) {
        if (clients.isEmpty()) {
            //throw new RuntimeException("clients.isEmpty()");
            logger.warn("clients.isEmpty()");
            return null;
        }
        int index = Math.abs(hash) % clients.size();
        final List<Integer> clientIds = clients.keySet().stream().sorted().collect(Collectors.toList());
        Integer clientId = clientIds.get(index);
        final Channel channel = clients.get(clientId);
        if (channel == null) {
            logger.warn("channel is null");
            return null;
        }
        return channel;
    }

    /**
     * 根据类型、权重选择一个客户端链接
     * @param serverType 服务器类型
     */
    public static Channel getChannelByTypeUseWeight(String serverType,
                                                    Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        int typeSize = serverTypeMap.size();
        if (typeSize < 1) {
            logger.warn("sendMsg2ServerByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return null;
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
                        logger.warn("getChannelByTypeUseWeight error 服务器未登录:{}", serverInfo.getId());
                        return null;
                    }
                    return selectChannelByHashKey(randomWeight, serverClients);
                }
                weightOffset += weight;
            }
            return null;
        }
        logger.warn("getChannelByTypeUseWeight useWeight=false 当前服务器类型未配置权重值:{}，将随机分配一个服务器", serverType);
        //如果权重值未配置，再随机选择一个服务器
        final Random random = new Random();
        int key = random.nextInt();
        return getChannelByTypeAndKey(serverType, key, clientsMapSupplier);
    }
}
