package com.hbsoo.server.session;

import com.hbsoo.server.NetworkClient;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.HBSPackage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内部服务器客户端，发送心跳给内部服务器服务端
 */
public final class HeartbeatSender {
    private static Logger logger = LoggerFactory.getLogger(HeartbeatSender.class);

    @Autowired
    private NetworkClient tcpNetworkClient;

    // 使用ScheduledExecutorService来定时发送心跳
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 每隔三秒向所有服务器发送心跳
    public void startHeartbeatSender() {
        scheduler.scheduleAtFixedRate(new HeartbeatTask(), 0, 3, TimeUnit.SECONDS);
    }

    // 停止心跳发送
    public void stopHeartbeatSender() {
        scheduler.shutdown();
    }

    // 心跳任务
    private class HeartbeatTask implements Runnable {

        @Override
        public void run() {
            try {
                final HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.InnerMessageType.HEARTBEAT);
                sendMsg2AllServer(builder);
            } catch (Exception e) {
                // 异常处理，例如记录日志
                System.err.println("Failed to send heartbeat: " + e.getMessage());
            }
        }
    }

    private void sendMsg2AllServer(HBSPackage.Builder msgBuilder) {
        // 构建消息包字节数组
        final byte[] msg = msgBuilder.buildPackage();
        try {
            // 遍历所有客户端连接，对每个连接通道执行写入和刷新操作，确保消息被发送到所有服务器
            // 使用同步机制保证线程安全，如果clients是被多线程访问和修改的
            InnerClientSessionManager.clients.forEach((serverType, e) -> {
                e.forEach((id, channel) -> {
                    try {
                        // 使用Unpooled.wrappedBuffer减少内存复制，提高性能
                        ByteBuf buf = Unpooled.wrappedBuffer(msg);
                        channel.writeAndFlush(buf).sync();
                    } catch (Exception ex) {
                        // 对异常进行处理，如记录日志
                        logger.error("Failed to send message to server: ", ex);
                        // 断开连接
                        InnerClientSessionManager.innerLogout(serverType, id);
                        InnerServerSessionManager.innerLogout(serverType, id);
                        // 重新链接
                        tcpNetworkClient.reconnect(serverType, id);
                    }
                });
            });
        } catch (Exception ex) {
            logger.error("An unexpected error occurred while sending message to servers: ", ex);
        }
    }
}