package com.hbsoo.server.message.client;

import com.hbsoo.server.message.HBSPackage;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/5/31.
 */
public abstract class InnerTcpClientMessageHandler implements InnerClientMessageHandler<ByteBuf> {


    /**
     * 只给分发器使用，业务处理器重写无效
     */
    @Override
    public void onMessage(ChannelHandlerContext ctx, ByteBuf msg) { }

    /**
     * 专门给业务处理器处理消息
     */
    public abstract void onMessage(ChannelHandlerContext ctx, HBSPackage.Decoder decoder);

    /**
     * 消息转发到【当前服务器】中的其他消息处理器中
     */
    public void redirectMessage(ChannelHandlerContext ctx, HBSPackage.Builder msgBuilder) {
        final byte[] buildPackage = msgBuilder.buildPackage();
        ByteBuf buf = Unpooled.wrappedBuffer(buildPackage);
        redirectMessage(ctx, buf);
    }

}
