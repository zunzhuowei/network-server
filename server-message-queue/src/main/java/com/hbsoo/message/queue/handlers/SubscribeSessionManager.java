package com.hbsoo.message.queue.handlers;


import com.hbsoo.server.config.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/27.
 */
final class SubscribeSessionManager {

    private static final Map<String, CopyOnWriteArraySet<String>> sessionMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(SubscribeSessionManager.class);

    /**
     * 保存订阅主题与服务器的关系
     * @param topic 主题
     * @param serverType 服务器类型
     * @param serverId 服务器id
     */
    public static void subscribe(String topic, String serverType, int serverId) {
        synchronized (sessionMap) {
            CopyOnWriteArraySet<String> topics = sessionMap.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
            topics.add(serverType + ":" + serverId);
        }
    }

    /**
     * 取消订阅
     * @param serverType 服务器类型
     * @param serverId 服务器id
     */
    public static void unSubscribe(String serverType, int serverId) {
        synchronized (sessionMap) {
            sessionMap.forEach((k, v) -> {
                boolean remove = v.remove(serverType + ":" + serverId);
                if (remove) {
                    logger.info("取消订阅关系 订阅服务器类型:{},订阅服务器id:{}", serverType, serverId);
                }
            });
            //CopyOnWriteArraySet<String> topics = sessionMap.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
            //topics.remove(serverType + ":" + serverId);
        }
    }

    /**
     * 根据主题和消息id获取分配的订阅服务器列表，
     * 保证每种服务器类型只有一个服务器消费。
     *
     * @param topic 主题
     * @return 服务器列表
     */
    public static List<ServerInfo> getSubscribeServers(String topic, long msgId) {
        CopyOnWriteArraySet<String> servers = sessionMap.get(topic);
        if (servers == null) {
            return new ArrayList<>();
        }
        Map<String, List<ServerInfo>> serverTypeMap = servers.parallelStream().map(s -> {
            String[] split = s.split(":");
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setType(split[0]);
            serverInfo.setId(Integer.parseInt(split[1]));
            return serverInfo;
        }).collect(Collectors.groupingBy(ServerInfo::getType));
        List<ServerInfo> result = new ArrayList<>();
        //根据消息id分配服务器
        for (String serverType : serverTypeMap.keySet()) {
            List<ServerInfo> serverInfos = serverTypeMap.get(serverType);
            serverInfos.sort(Comparator.comparingInt(ServerInfo::getId));
            int index = (int) (msgId % serverInfos.size());
            final ServerInfo serverInfo = serverInfos.get(index);
            result.add(serverInfo);
        }
        return result;
    }

}
