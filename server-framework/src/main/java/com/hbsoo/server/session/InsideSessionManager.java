package com.hbsoo.server.session;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/11.
 */
class InsideSessionManager {

    // 使用slf4j作为日志记录工具
    private static final Logger logger = LoggerFactory.getLogger(InsideSessionManager.class);

    public static void login(String serverType, Integer serverId, Channel channel, int index,
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
    public static void logout(String serverType, Integer serverId,
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
    public static void logoutWithChannel(Channel channel,
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
        final Random random = new Random();
        int key = random.nextInt();
        return getChannelByTypeAndIdKey(serverId, serverType, key, clientsMapSupplier);
    }

    /**
     * 根据serverId、ServerType、key获取channel
     * @param serverId 服务器id
     * @param serverType 服务器类型
     * @param key  根据键值进行客户端选择的键，用于计算哈希值以选择具体客户端。
     */
    public static Channel getChannelByTypeAndIdKey(int serverId, String serverType, Object key,
                                                      Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        if (clients.isEmpty()) {
            logger.error("getChannelByTypeAndIdKey error 服务器未登录:{}", serverId);
            return null;
        }
        int hash = key.hashCode();
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
        return tryGetAvailableChannel(serverType, key, typeSize, null, false, serverTypeMap);
    }

    /**
     * 获取可用的channel,如果没有可用的channel，则返回null;
     * key计算出的服务器不可用时，重新选举相同类型的可用服务器链接作为备选；
     * @param serverType 服务器类型
     * @param key 根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     */
    public static Channel getAvailableChannelByTypeAndKey(String serverType, Object key,
                                                 Supplier<Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>>> clientsMapSupplier) {
        if (key == null) {
            throw new RuntimeException("key is null");
        }
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>> clientsMap = clientsMapSupplier.get();
        //判断使用哪个服务器
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap = clientsMap.computeIfAbsent(serverType, k -> new ConcurrentHashMap<>());
        int typeSize = serverTypeMap.size();
        return tryGetAvailableChannel(serverType, key, typeSize, new ArrayList<>(), true, serverTypeMap);
    }
    /**
     * 根据服务器类型和key获取channel,
     * 如果选取的服务器不可用，则重新选举相同类型的服务器
     * @param serverType 服务器类型
     * @param key  根据键值进行服务器选择的键，用于计算哈希值以选择具体服务器。
     * @param typeSize 服务器类型数量
     * @param excludedServerIds 排除的服务器id
     * @param serverTypeMap 服务器类型map
     * @return channel
     */
    private static Channel tryGetAvailableChannel(String serverType, Object key, int typeSize, List<Integer> excludedServerIds,
                                      boolean isRetry,
                                      ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>> serverTypeMap) {
        if (typeSize < 1) {
            logger.info("getChannelByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return null;
        }
        int hash = key.hashCode();
        int serverIndex = Math.abs(hash) % typeSize;
        final List<Integer> serverIds = serverTypeMap.keySet().stream()
                .filter(k -> Objects.isNull(excludedServerIds) || !excludedServerIds.contains(k))//排除
                .sorted().collect(Collectors.toList());
        Integer serverId = serverIds.get(serverIndex);
        //根据key的hash值判断使用哪个客户端
        ConcurrentHashMap<Integer, Channel> clients = serverTypeMap.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        final Channel channel = selectChannelByHashKey(hash, clients);
        if (channel == null && isRetry) {
            excludedServerIds.add(serverId);
            return tryGetAvailableChannel(serverType, key, --typeSize, excludedServerIds, true, serverTypeMap);
        }
        return channel;
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
        if (!channel.isActive()) {
            logger.warn("channel is isActive");
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
            logger.info("sendMsg2ServerByTypeAndKey typeSize < 1, serverType:{}", serverType);
            return null;
        }
        //当前服务器客户端登录到的其他服务器
        List<ServerInfo> insideServers = NowServer.getInsideServers().stream()
                .filter(serverInfo -> serverInfo.getType().equals(serverType))
                .collect(Collectors.toList());
        boolean useWeight = insideServers.stream().anyMatch(serverInfo -> serverInfo.getWeight() > 0);
        if (useWeight) {
            //根据权重比例随机选择一个服务器
            insideServers.sort(Comparator.comparingInt(ServerInfo::getWeight));
            int weightSum = insideServers.stream()
                    .map(ServerInfo::getWeight)
                    .mapToInt(Integer::intValue)
                    .sum();
            int randomWeight = new Random().nextInt(weightSum);
            int weightOffset = 0;
            for (int i = 0; i < insideServers.size(); i++) {
                ServerInfo serverInfo = insideServers.get(i);
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
