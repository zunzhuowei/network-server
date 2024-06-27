package com.hbsoo.message.queue;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.session.InnerClientSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
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
        List<ServerInfo> innerServers = NowServer.getInnerServers();
        if (innerServers.isEmpty()) {
            logger.warn("innerServers is empty");
            return;
        }
        boolean exist = innerServers.stream().anyMatch(serverInfo -> serverInfo.getType().equals(queueServerTypeName));
        if (!exist) {
            logger.warn("queueServerTypeName is not exist:{}", queueServerTypeName);
            return;
        }
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .writeStr(objJson, topic)
                .msgType(HBSMessageType.Inner.PUBLISH);
        InnerClientSessionManager.forwardMsg2ServerByTypeAndKeyUseSender
                (builder, queueServerTypeName, objJson);
    }

}
