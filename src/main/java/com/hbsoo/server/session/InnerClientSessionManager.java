package com.hbsoo.server.session;

import com.hbsoo.server.NetworkClient;
import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/5.
 */
public final class InnerClientSessionManager {

    // 使用slf4j作为日志记录工具
    private static Logger logger = LoggerFactory.getLogger(InnerClientSessionManager.class);


    /**
     * 向所有服务器发送消息。
     * 使用HBSPackage.Builder构建消息包，然后将该消息包发送给所有已连接的服务器。
     * 此方法利用Netty的ByteBuf来高效地处理字节流，确保消息能够被正确地序列化和发送。
     *
     * @param msgBuilder 消息包的构建器，用于创建待发送的消息包。
     */
    public static void sendMsg2AllServer(HBSPackage.Builder msgBuilder) {
        // 构建消息包字节数组
        final byte[] msg = msgBuilder.buildPackage();

        // 使用Unpooled.wrappedBuffer减少内存复制，提高性能
        ByteBuf buf = Unpooled.wrappedBuffer(msg);

        try {
            // 遍历所有客户端连接，对每个连接通道执行写入和刷新操作，确保消息被发送到所有服务器
            // 使用同步机制保证线程安全，如果clients是被多线程访问和修改的
            //synchronized (NetworkClient.class) {
                NetworkClient.clients.values().forEach(e -> e.values().forEach(channel -> {
                    try {
                        channel.writeAndFlush(buf);
                    } catch (Exception ex) {
                        // 对异常进行处理，如记录日志
                        logger.error("Failed to send message to server: ", ex);
                        // 可以根据需要决定是否需要对发送失败的服务器进行重试等操作
                    }
                }));
            //}
        } catch (Exception ex) {
            logger.error("An unexpected error occurred while sending message to servers: ", ex);
        } finally {
            buf.release();
        }
    }


    /**
     * 根据服务器类型向指定服务器发送消息。
     * 此方法用于构建消息包并将其发送到由服务器类型指定的目标服务器。
     *
     * @param msgBuilder 消息包的构建器，用于组装待发送的消息。
     * @param serverType 目标服务器的类型，用于确定消息发送的目标。
     */
    public static void sendMsg2ServerByType(HBSPackage.Builder msgBuilder, ServerType serverType) {
        // 构建消息包
        final byte[] msg = msgBuilder.buildPackage();

        // 将消息包复制到ByteBuf中，以便于网络传输
        ByteBuf buf = Unpooled.wrappedBuffer(msg);

        // 获取与指定服务器类型对应的服务器集合
        final ConcurrentHashMap<Integer, Channel> servers = NetworkClient.clients.get(serverType);
        // 此处注释提示：接下来应有代码逻辑用于从servers中选择一个特定的Channel，并通过该Channel发送buf中的消息
        try {
            servers.values().forEach(channel -> {
                try {
                    channel.writeAndFlush(buf);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Failed to send message to server: ", e);
                }
            });
        } finally {
            buf.release();
        }
    }

    /**
     * 根据服务器ID向指定服务器发送消息。
     * 此方法用于构建消息包并将其发送到由服务器ID指定的目标服务器。
     * 它首先将消息包构建为字节数组，然后创建一个ByteBuf对象来封装这个字节数组。
     * 最后，它遍历所有客户端连接，并找到与目标服务器ID匹配的连接，将消息写入并刷新到该连接。
     *
     * @param msgBuilder 消息包的构建器，用于构建待发送的消息包。
     * @param serverId 目标服务器的ID，用于指定消息发送的目的地。
     */
    public static void sendMsg2ServerByServerId(HBSPackage.Builder msgBuilder, int serverId) {
        // 构建消息包字节数组
        final byte[] msg = msgBuilder.buildPackage();
        // 创建一个ByteBuf对象，并将消息包字节数组复制到其中
        ByteBuf buf = Unpooled.wrappedBuffer(msg);
        try {
            // 遍历所有客户端连接，找到目标服务器ID对应的连接，然后发送消息
            NetworkClient.clients.values().forEach(e -> {
                try {
                    e.get(serverId).writeAndFlush(buf);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    logger.error("Failed to send message to server: ", exception);
                }
            });
        } finally {
            // 无论发送操作是否成功，都释放ByteBuf资源
            buf.release();
        }
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
        // 构建消息包字节数组
        final byte[] msg = msgBuilder.buildPackage();
        // 创建一个ByteBuf对象，并将消息包字节数组复制到其中
        ByteBuf buf = Unpooled.wrappedBuffer(msg);
        try {
            // 遍历所有客户端连接，找到目标服务器ID对应的连接，然后发送消息
            final ConcurrentHashMap<Integer, Channel> servers = NetworkClient.clients.get(serverType);
            if (servers == null) {
                return;
            }
            final Channel channel = servers.get(serverId);
            if (channel == null) {
                return;
            }
            try {
                channel.writeAndFlush(buf);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to send message to server: ", e);
            }
        }finally {
            buf.release();
        }
    }

    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, ServerType serverType, String key) {
        final byte[] msg = msgBuilder.buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(msg);
        //根据key的hash值判断使用哪个服务器
        try {
            final ConcurrentHashMap<Integer, Channel> servers = NetworkClient.clients.get(serverType);
            if (servers == null) {
                return;
            }
            final Channel channel = servers.get(key.hashCode() % servers.size());
            if (channel == null) {
                return;
            }
            try {
                channel.writeAndFlush(buf);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to send message to server: ", e);
            }
        }finally {
            buf.release();
        }
    }
    public static void sendMsg2ServerByTypeAndKey(HBSPackage.Builder msgBuilder, ServerType serverType, Long id) {
        sendMsg2ServerByTypeAndKey(msgBuilder, serverType, String.valueOf(id));
    }
}
