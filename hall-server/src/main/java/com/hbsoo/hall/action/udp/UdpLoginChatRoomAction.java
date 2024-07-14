package com.hbsoo.hall.action.udp;

import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zun.wei on 2024/6/15.
 */
@OutsideMessageHandler(value = 100, protocol = Protocol.UDP)
public class UdpLoginChatRoomAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(UdpLoginChatRoomAction.class);

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_WEBSOCKET, decoder);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return decoder.readStr();
    }
}
