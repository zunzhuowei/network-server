package com.hbsoo.server.action.mqtt;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/27.
 */
final class MqttSubscribeSessionManager {

    private static final Map<String, CopyOnWriteArraySet<SubscribeInfo>> sessionMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscribeSessionManager.class);

    /**
     * 保存订阅主题与服务器的关系
     * @param topic 主题
     */
    public static void subscribe(String topic, SubscribeInfo subscribeInfo) {
        synchronized (sessionMap) {
            CopyOnWriteArraySet<SubscribeInfo> topics = sessionMap.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>());
            topics.add(subscribeInfo);
        }
    }

    /**
     * 取消订阅
     * @param userChannelId mqtt channel id
     */
    public static void unSubscribe(String userChannelId) {
        synchronized (sessionMap) {
            sessionMap.forEach((k, v) -> {
                v.parallelStream().forEach(s -> {
                    if (s.getUserChannelId().equals(userChannelId)) {
                        v.remove(s);
                    }
                    logger.info("取消订阅关系 mqtt channelId:{}", userChannelId);
                });
            });
        }
    }

    /**
     * 根据主题和消息id获取分配的订阅客户端列表，
     *
     * @param topic 主题
     * @return 订阅列表
     */
    public static List<SubscribeInfo> getSubscribeServers(String topic) {
        CopyOnWriteArraySet<SubscribeInfo> servers = sessionMap.get(topic);
        if (servers == null) {
            return new ArrayList<>();
        }
        return servers.parallelStream().collect(Collectors.toList());
    }

}
