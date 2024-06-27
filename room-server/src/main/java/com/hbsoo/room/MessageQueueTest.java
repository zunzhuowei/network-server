package com.hbsoo.room;

import com.hbsoo.message.queue.config.MessageListener;
import com.hbsoo.message.queue.QueueMessageHandler;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;

/**
 * Created by zun.wei on 2024/6/27.
 */
@MessageListener(topic = "test", serverType = "hall")
public class MessageQueueTest implements QueueMessageHandler {


    @Override
    public boolean handle(String objJson) {
        final ServerInfo serverInfo = NowServer.getServerInfo();
        System.out.println("objJson = " + objJson);
        System.out.println("serverInfo = " + serverInfo.getId());
        return false;
    }


}
