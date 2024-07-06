package com.hbsoo.server.action.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.ProtocolType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.utils.HttpRequestParser;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 接收网关转发到内网服务器中的websocket、tcp、udp消息，
 * 再路由到相应的tcp处理器中
 * Created by zun.wei on 2024/6/15.
 */
@InnerServerMessageHandler(HBSMessageType.Inner.GATEWAY_ROUTING_WEBSOCKET_TCP_UDP_TO_INNER_SERVER)
public class GatewayWebsocketTcpUdpMessageRoutingAction extends ServerMessageDispatcher {


    @Override
    public void handle(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
        byte[] header = decoder.readBytes();
        byte[] body = decoder.readBytes();
        byte[] _package = new byte[header.length + body.length + 4];
        System.arraycopy(header, 0, _package, 0, header.length);//写入header
        System.arraycopy(toHH(body.length), 0, _package, header.length, 4);//写入消息长度
        System.arraycopy(body, 0, _package, header.length + 4, body.length);//写入消息体
        HBSPackage.Decoder d = HBSPackage.Decoder
                .withHeader(header)
                .readPackageBody(_package);
        redirectAndSwitchProtocol(ctx, ProtocolType.OUTER_WEBSOCKET, d);
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, HBSPackage.Decoder decoder) {
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
