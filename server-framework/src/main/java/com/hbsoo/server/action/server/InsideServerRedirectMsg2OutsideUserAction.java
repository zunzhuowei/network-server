package com.hbsoo.server.action.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * 接收内部服务器的消息，转发给登录的外部客户
 * Created by zun.wei on 2024/6/6.
 */
@InsideServerMessageHandler(MessageType.Inside.REDIRECT)
public class InsideServerRedirectMsg2OutsideUserAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(InsideServerRedirectMsg2OutsideUserAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        String protocolStr = decoder.readStr();
        OutsideUserProtocol protocol = OutsideUserProtocol.valueOf(protocolStr);
        if (protocol == OutsideUserProtocol.HTTP) {
            String contentType = decoder.readStr();
            int status = decoder.readInt();
            byte[] insidePackage = decoder.readBytes();
            String headerMapStr = decoder.readStr();
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
            Map<String, String> map = gson.fromJson(headerMapStr, Map.class);
            ExtendBody extendBody = decoder.readExtendBody();
            logger.debug("protocol:{},expandBody:{}", protocol, extendBody);
            outsideUserSessionManager.httpResponse(map, insidePackage, contentType, extendBody, HttpResponseStatus.valueOf(status));
            return;
        }
        long id = decoder.readLong();
        byte[] insidePackage = decoder.readBytes();
        logger.debug("id:{} protocol:{}", id, protocol);
        outsideUserSessionManager.sendMsg2User(protocol, insidePackage, id);
    }


    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }
}
