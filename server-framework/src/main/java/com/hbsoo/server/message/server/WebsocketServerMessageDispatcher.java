package com.hbsoo.server.message.server;

import com.google.gson.Gson;
import com.hbsoo.server.annotation.InnerServerMessageHandler;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.message.TextWebSocketPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/6.
 */
abstract class WebsocketServerMessageDispatcher implements ServerMessageHandler<WebSocketFrame> {

    protected static final Map<Integer, WebsocketServerMessageDispatcher> innerWebsocketServerDispatchers = new ConcurrentHashMap<>();
    protected static final Map<Integer, WebsocketServerMessageDispatcher> outerWebsocketServerDispatchers = new ConcurrentHashMap<>();
    @Autowired
    private ThreadPoolScheduler innerServerThreadPoolScheduler;
    @Qualifier("outerServerThreadPoolScheduler")
    @Autowired(required = false)
    private ThreadPoolScheduler outerServerThreadPoolScheduler;
    @Autowired
    private SpringBeanFactory springBeanFactory;

    @PostConstruct
    protected void init() {
        final boolean innerDispatcher = isInnerDispatcher();
        final Map<String, Object> handlers = springBeanFactory.getBeansWithAnnotation(innerDispatcher ? InnerServerMessageHandler.class : OuterServerMessageHandler.class);
        handlers.values().stream().filter(handler -> {
            return handler instanceof WebsocketServerMessageDispatcher;
        }).forEach(handler -> {
            WebsocketServerMessageDispatcher h = (WebsocketServerMessageDispatcher) handler;
            if (innerDispatcher) {
                final InnerServerMessageHandler annotation = handler.getClass().getAnnotation(InnerServerMessageHandler.class);
                innerWebsocketServerDispatchers.putIfAbsent(annotation.value(), h);
            } else {
                final OuterServerMessageHandler annotation = handler.getClass().getAnnotation(OuterServerMessageHandler.class);
                outerWebsocketServerDispatchers.putIfAbsent(annotation.value(), h);
            }
        });
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        try {
            final boolean innerDispatcher = isInnerDispatcher();
            Map<Integer, WebsocketServerMessageDispatcher> dispatcherMap = innerDispatcher ? innerWebsocketServerDispatchers : outerWebsocketServerDispatchers;

            final ByteBuf msg = webSocketFrame.content();
            byte[] received = new byte[msg.readableBytes()];
            msg.readBytes(received);
            //TextWebSocketFrame
            boolean isText = webSocketFrame instanceof TextWebSocketFrame;
            if (isText) {
                String jsonStr = new String(received);
                Gson gson = new Gson();
                final TextWebSocketPackage socketPackage = gson.fromJson(jsonStr, TextWebSocketPackage.class);
                final int msgType = socketPackage.getMsgType();
                //把文本消息，转成json格式
                received = HBSPackage.Builder.withDefaultHeader().msgType(msgType).writeStr(jsonStr).buildPackage();
            }

            // BinaryWebSocketFrame
            final HBSPackage.Decoder decoder = HBSPackage.Decoder.withDefaultHeader().readPackageBody(received);
            final int msgType = decoder.readMsgType();
            final WebsocketServerMessageDispatcher dispatcher = dispatcherMap.get(msgType);
            if (Objects.nonNull(dispatcher)) {
                if (innerDispatcher) {
                    innerServerThreadPoolScheduler.execute(dispatcher.threadKey(decoder), () -> {
                        dispatcher.onMessage(ctx, decoder);
                    });
                } else {
                    outerServerThreadPoolScheduler.execute(dispatcher.threadKey(decoder), () -> {
                        dispatcher.onMessage(ctx, decoder);
                    });
                }
            } else {
                try {
                    String _404 = "404";
                    WebSocketFrame response = isText ? new TextWebSocketFrame(_404) : new BinaryWebSocketFrame(Unpooled.wrappedBuffer(_404.getBytes()));
                    ctx.writeAndFlush(response).sync();
                    ctx.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
        //onMessage(ctx, decoder);
    }

    /**
     * 是否为内部消息
     * @return boolean true 内部消息，false 外部消息
     */
    public abstract boolean isInnerDispatcher();

    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);
}
