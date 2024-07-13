package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * 接收网关转发到内网服务器中的websocket、tcp、udp消息，
 * 再路由到相应的tcp处理器中
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(MessageType.Inside.GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER)
public class RoutingGatewayWebsocketTcpUdpMessageAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        //byte[] header = decoder.readBytes();
        //byte[] body = decoder.readBytes();
        //byte[] _package = new byte[header.length + body.length + 4];
        //System.arraycopy(header, 0, _package, 0, header.length);//写入header
        //System.arraycopy(toHH(body.length), 0, _package, header.length, 4);//写入消息长度
        //System.arraycopy(body, 0, _package, header.length + 4, body.length);//写入消息体
        //NetworkPacket.Decoder d = NetworkPacket.Decoder
        //        .withHeader(header)
        //        .parsePacket(_package);
        //redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_WEBSOCKET, d);
        ExtendBody extendBody = decoder.readExtendBody();
        int msgType = decoder.readExtendBodyMode().readInt();
        byte[] header = decoder.getHeader();
        decoder.resetBodyReadOffset();
        NetworkPacket.Builder builder = decoder.toBuilder(header).msgType(msgType);
        redirectAndSwitchProtocol(ctx, ProtocolType.OUTSIDE_WEBSOCKET, builder);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        return null;
    }

    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

}
