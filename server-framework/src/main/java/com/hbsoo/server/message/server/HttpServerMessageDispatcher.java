package com.hbsoo.server.message.server;

import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.HttpPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class HttpServerMessageDispatcher implements ServerMessageHandler<FullHttpRequest> {

    protected static final Map<String, HttpServerMessageDispatcher> innerHttpServerDispatchers = new ConcurrentHashMap<>();
    protected static final Map<String, HttpServerMessageDispatcher> outerHttpServerDispatchers = new ConcurrentHashMap<>();
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;
    @Qualifier("outerServerThreadPoolScheduler")
    @Autowired(required = false)
    private ThreadPoolScheduler outerServerThreadPoolScheduler;

    @PostConstruct
    protected void init() {
        final boolean innerDispatcher = isInnerDispatcher();
        final Map<String, Object> handlers = SpringBeanFactory.getBeansWithAnnotation(innerDispatcher ? InnerServerMessageHandler.class : OuterServerMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            return handler instanceof HttpServerMessageDispatcher;
        }).forEach(handler -> {
            HttpServerMessageDispatcher h = (HttpServerMessageDispatcher) handler;
            if (innerDispatcher) {
                final InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                innerHttpServerDispatchers.put(annotation.uri(), h);
            } else {
                final OuterServerMessageHandler annotation = handler.getClass().getAnnotation(OuterServerMessageHandler.class);
                outerHttpServerDispatchers.put(annotation.uri(), h);
            }
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, FullHttpRequest msg) {
        final String path;
        HttpPackage httpPackage = new HttpPackage();
        try {
            final String uri = msg.uri();
            final HttpMethod method = msg.method();
            final HttpHeaders headers = msg.headers();
            final int index = uri.indexOf("?");
            path = index < 0 ? uri : uri.substring(0, index);
            //System.out.println("path:" + path);
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            final Map<String, List<String>> parameters = decoder.parameters();
            final ByteBuf content = msg.content();
            final boolean readable = content.isReadable();
            httpPackage.setHeaders(headers);
            httpPackage.setParameters(parameters);
            httpPackage.setPath(path);
            httpPackage.setUri(uri);
            httpPackage.setFullHttpRequest(msg);
            httpPackage.setMethod(method.name());
            if (readable) {
                byte[] received = new byte[content.readableBytes()];
                content.readBytes(received);
                httpPackage.setBody(received);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ctx.channel().closeFuture().sync();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            return;
        }
        final boolean innerDispatcher = isInnerDispatcher();
        Map<String, HttpServerMessageDispatcher> dispatcherMap = innerDispatcher ? innerHttpServerDispatchers : outerHttpServerDispatchers;
        final HttpServerMessageDispatcher dispatcher = dispatcherMap.get(path);
        if (Objects.nonNull(dispatcher)) {
            if (innerDispatcher) {
                innerServerThreadPoolScheduler.execute(dispatcher.threadKey(null), () -> {
                    dispatcher.onMessage(ctx, httpPackage);
                    ctx.close();
                });
            } else {
                outerServerThreadPoolScheduler.execute(dispatcher.threadKey(null), () -> {
                    dispatcher.onMessage(ctx, httpPackage);
                    ctx.close();
                });
            }
        } else {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.NOT_FOUND);
            try {
                ctx.writeAndFlush(response).sync();
                ctx.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //onMessage(ctx, decoder);
    }

    public abstract boolean isInnerDispatcher();


    public abstract void onMessage(ChannelHandlerContext ctx, HttpPackage httpPackage);

}
