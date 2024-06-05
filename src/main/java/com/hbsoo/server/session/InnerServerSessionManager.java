package com.hbsoo.server.session;

import com.hbsoo.server.message.HBSPackage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/5/31.
 */
public final class InnerServerSessionManager {

    // 使用slf4j作为日志记录工具
    private static Logger logger = LoggerFactory.getLogger(InnerServerSessionManager.class);

    public static Map<ServerType, ConcurrentHashMap<Integer, Channel>> clients = new ConcurrentHashMap<>();


    public static void innerLogin(ServerType serverType, Integer serverId, Channel channel) {
        final ConcurrentHashMap<Integer, Channel> servers = clients.get(serverType);
        if (servers != null) {
            servers.put(serverId, channel);
        } else {
            clients.put(serverType, new ConcurrentHashMap<>());
            clients.get(serverType).put(serverId, channel);
        }
    }

    public static void sendMsg2ServerByType(HBSPackage.Builder msgBuilder, ServerType serverType) {
        final ConcurrentHashMap<Integer, Channel> servers = clients.get(serverType);
        if (servers != null) {
            servers.forEach((serverId, channel) -> {
                try {
                    channel.writeAndFlush(msgBuilder.buildPackage());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("sendMsg2ServerByType error:{}", e.getMessage());
                }
            });
        }
    }

    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId) {
        clients.forEach((serverType, servers) -> {
            final Channel channel = servers.get(serverId);
            if (channel != null) {
                try {
                    channel.writeAndFlush(msgBuilder.buildPackage());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("sendMsg2ServerByServerId error:{}", e.getMessage());
                }
            }
        });
    }

    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId, ServerType serverType) {
        final ConcurrentHashMap<Integer, Channel> servers = clients.get(serverType);
        if (servers != null) {
            final Channel channel = servers.get(serverId);
            if (channel != null) {
                try {
                    channel.writeAndFlush(msgBuilder.buildPackage());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("sendMsg2ServerByServerId error:{}", e.getMessage());
               }
           }
       }
   }

   public static void sendMsg2AllServer(HBSPackage.Builder msgBuilder) {
       clients.forEach((serverType, servers) -> {
           servers.forEach((serverId, channel) -> {
               try {
                   channel.writeAndFlush(msgBuilder.buildPackage());
               } catch (Exception e) {
                   e.printStackTrace();
                   logger.error("sendMsg2AllServer error:{}", e.getMessage());
               }
           });
       });
   }

    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, ServerType serverType, String key) {
        final ConcurrentHashMap<Integer, Channel> servers = clients.get(serverType);
        //根据key的hash值判断使用哪个服务器
        if (servers != null) {
            int hash = key.hashCode();
            int serverId = hash % servers.size();
            final Channel channel = servers.get(serverId);
            if (channel != null) {
                try {
                    channel.writeAndFlush(msgBuilder.buildPackage());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("sendMsg2ServerByTypeAndKey error:{}", e.getMessage());
                }
            }
       }
    }

    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, ServerType serverType, Long id) {
        sendMsg2ServerByTypeAndKey(msgBuilder, serverType, String.valueOf(id));
    }

}
