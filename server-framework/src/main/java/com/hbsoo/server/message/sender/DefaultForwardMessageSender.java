package com.hbsoo.server.message.sender;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.entity.ForwardMessage;

import java.util.List;

/**
 * Created by zun.wei on 2024/6/19.
 */
public final class DefaultForwardMessageSender extends AbstractForwardMessageSender {


    @Override
    public List<ForwardMessage> loadFromDb() {
        final ServerInfo serverInfo = NowServer.getServerInfo();
        final Integer serverId = serverInfo.getId();
        // load from db by serverId
        return null;
    }

    @Override
    public void save2Db(ForwardMessage message) {

    }

    @Override
    public void removeFromDb(Long id) {

    }


}
