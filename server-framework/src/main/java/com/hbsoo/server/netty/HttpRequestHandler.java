package com.hbsoo.server.netty;

import com.hbsoo.server.message.server.ServerMessageHandler;
import com.hbsoo.server.session.ChannelManager;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import com.hbsoo.server.utils.SpringBeanFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerMessageHandler handler;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HttpRequestHandler(ServerMessageHandler handler) {
        this.handler = handler;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        handler.onMessage(ctx, request);
        //超时响应30s
        DelayThreadPoolScheduler scheduler = SpringBeanFactory.getBean(DelayThreadPoolScheduler.class);
        scheduler.schedule(() -> {
            Optional<Channel> optional = ChannelManager.getChannel(ctx.channel().id().asLongText());
            if (optional.isPresent()) {
                String uri = request.uri();
                OutsideUserSessionManager sessionManager = SpringBeanFactory.getBean(OutsideUserSessionManager.class);
                sessionManager.response(ctx.channel(), null, null,
                        "application/json; charset=UTF-8", HttpResponseStatus.REQUEST_TIMEOUT, null);
                logger.warn("超时响应,uri:{}", uri);
            }
        }, 30, TimeUnit.SECONDS);
    }

}