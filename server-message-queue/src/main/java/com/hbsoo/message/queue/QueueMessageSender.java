package com.hbsoo.message.queue;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import com.hbsoo.server.utils.SpringBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 【客户端测】
 * Created by zun.wei on 2024/6/27.
 */
public final class QueueMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(QueueMessageSender.class);

    /**
     * 发布消息到队列中
     * @param queueServerTypeName 队列服务器类型名称
     * @param topic 消息主题
     * @param objJson 消息内容，建议json格式数据。
     */
    public static void publish(String queueServerTypeName, String topic, String objJson) {
        List<ServerInfo> insideServers = NowServer.getInsideServers();
        if (insideServers.isEmpty()) {
            logger.warn("insideServers is empty");
            return;
        }
        boolean exist = insideServers.stream().anyMatch(serverInfo -> serverInfo.getType().equals(queueServerTypeName));
        if (!exist) {
            logger.warn("queueServerTypeName is not exist:{}", queueServerTypeName);
            return;
        }
        SnowflakeIdGenerator snowflakeIdGenerator = SpringBeanFactory.getBean(SnowflakeIdGenerator.class);
        long msgId = snowflakeIdGenerator.generateId();
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .writeLong(msgId)
                .writeStr(objJson)
                .writeStr(topic)
                .writeStr(NowServer.getServerInfo().getType())
                .writeInt(NowServer.getServerInfo().getId())
                .msgType(MessageType.Inside.PUBLISH);
        InsideClientSessionManager.forwardMsg2ServerByTypeAndKeyUseSender(builder, queueServerTypeName, msgId);
    }

}
