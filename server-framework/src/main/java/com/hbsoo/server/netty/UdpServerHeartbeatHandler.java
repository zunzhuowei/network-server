package com.hbsoo.server.netty;

import com.hbsoo.server.session.ChannelManager;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 长时间没有收到客户端的消息，则断开链接；
 */
public final class UdpServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UdpServerHeartbeatHandler.class);
    private static final Map<String, AtomicInteger> idleTimes = new ConcurrentHashMap<>();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                idleTimes.forEach((k, v) -> {
                    if (v.get() >= 3) {
                        logger.warn("Channel inactive, closing connection,remoteAddress:{}", k);
                        String senderHost = k.split(":")[0];
                        int senderPort = Integer.parseInt(k.split(":")[1]);
                        ChannelManager.removeUdpChannel(senderHost, senderPort);
                        OutsideUserSessionManager manager = SpringBeanFactory.getBean(OutsideUserSessionManager.class);
                        Long userId = manager.getUdpRelativeUserId(senderHost, senderPort);
                        if (Objects.nonNull(userId)) {
                            manager.logoutAndSyncAllServer(userId);
                        }
                        idleTimes.remove(k);
                    } else {
                        v.incrementAndGet();
                        logger.trace("not received heartbeat from client:{},times:{}", k, v.get());
                    }
                });
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof DatagramPacket)) {
            ctx.fireChannelRead(msg);
            return;
        }
        DatagramPacket packet = (DatagramPacket) msg;
        String senderHost = packet.sender().getHostString();
        int senderPort = packet.sender().getPort();
        ChannelManager.addUdpChannel(senderHost, senderPort, ctx.channel());
        idleTimes.computeIfAbsent(senderHost + ":" + senderPort, k -> new AtomicInteger(0)).incrementAndGet();
        //流转到下一个handler
        super.channelRead(ctx, msg);
    }

}
