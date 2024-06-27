package com.hbsoo.message.queue.handlers;


import com.hbsoo.server.config.ServerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/27.
 */
final class SubscribeSessionManager {

    private static final Map<String, CopyOnWriteArraySet<String>> sessionMap = new ConcurrentHashMap<>();

    /**
     * 保存订阅主题与服务器的关系
     * @param topic 主题
     * @param serverType 服务器类型
     * @param serverId 服务器id
     */
    public static void subscribe(String topic, String serverType, int serverId) {
        CopyOnWriteArraySet<String> topics = sessionMap.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
        topics.add(serverType + ":" + serverId);
    }

    /**
     * 取消订阅
     * @param topic 主题
     * @param serverType 服务器类型
     * @param serverId 服务器id
     */
    public static void unSubscribe(String topic, String serverType, int serverId) {
        CopyOnWriteArraySet<String> topics = sessionMap.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
        topics.remove(serverType + ":" + serverId);
    }

    /**
     * 根据主题获取订阅的服务器列表
     * @param topic 主题
     * @return 服务器列表
     */
    public static List<ServerInfo> getSubscribeServers(String topic) {
        final CopyOnWriteArraySet<String> strings = sessionMap.get(topic);
        if (strings == null) {
            return new ArrayList<>();
        }
        return strings.parallelStream().map(s -> {
            String[] split = s.split(":");
            ServerInfo serverInfo = new ServerInfo();
            serverInfo.setType(split[0]);
            serverInfo.setId(Integer.parseInt(split[1]));
            return serverInfo;
        }).collect(Collectors.toList());
    }

}
