package com.hbsoo.server.message.sender;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.entity.ForwardMessage;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.session.InsideClientSessionManager;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/6/19.
 */
public abstract class AbstractForwardMessageSender implements ForwardMessageSender {

    @Autowired
    private DelayThreadPoolScheduler delayThreadPoolScheduler;
    private static final Logger logger = LoggerFactory.getLogger(AbstractForwardMessageSender.class);

    private long now() {
        return System.currentTimeMillis();
    }
    //1.接收到消息，先把消息保存db中
    //2.再将消息放入调度器中等待到时发送出去
    //3.如果发送成功，删除db中的消息
    //4.如果发送失败，重新放入调度器中等待再次发送，直到发送成功或这消息到期，再删除db中的消息
    //5.下一个服务收到消息，重复步骤1，2，3，4，5，或者将消息在该服务器消费掉，删除db中的消息
    //6.当服务器启动时，读取db中的属于自己的消息，重复步骤2-5；

    @Override
    public void send(ForwardMessage message) {
        save2Db(message);
        execSend(message);
    }

    private void execSend(ForwardMessage message) {
        long now = now();
        long forwardTime = message.getForwardTime();
        long expireTime = message.getExpireTime();
        Long id = message.getId();
        String toServerType = message.getToServerType();
        Object forwardKey = message.getForwardKey();
        Integer toServerId = message.getToServerId();
        Boolean useAvailableServer = message.getUseAvailableServer();
        byte[] originMessage = message.getOriginMessage();
        if ((now > expireTime) && expireTime != -1) {
            removeFromDb(id);
            return;
        }
        delayThreadPoolScheduler.schedule(() -> {
            Channel channel = toServerId > 0 ?
                    //指定服务器类型和id
                    InsideClientSessionManager.getChannelByServerTypeAndId(toServerId, toServerType) :
                    !useAvailableServer ?
                            // 指定服务器类型和key计算出来的服务器
                            InsideClientSessionManager.getChannelByTypeAndKey(toServerType, forwardKey) :
                            // 指定服务器类型和key计算出来的服务器，如遇不可用服务器，尝试获取可用服务器
                            InsideClientSessionManager.getAvailableChannelByTypeAndKey(toServerType, forwardKey);
            if (channel != null) {
                NetworkPacket.Decoder decoder = NetworkPacket.Decoder
                        .withHeader(NetworkPacket.TCP_HEADER)
                        .parsePacket(originMessage);
                int msgType = decoder.getMsgType();
                NetworkPacket.Builder builder = decoder.toBuilder().msgType(msgType);
                builder.sendTcpTo(channel, future -> {
                    if (!future.isSuccess()) {
                        logger.warn("forward fail, toServerType:{}, forwardKey:{},msgType:{},retry 3 seconds after!", toServerType, forwardKey, msgType);
                        resend(message);
                    } else {
                        if (toServerId > 0) {
                            logger.debug("forward use toServerId, toServerType:{},toServerId:{},msgType:{},", toServerType, toServerId, msgType);
                        } else {
                            logger.debug("forward use forwardKey, toServerType:{}, forwardKey:{},msgType:{},", toServerType, forwardKey, msgType);
                        }
                        removeFromDb(id);
                    }
                });
            } else {
                List<ServerInfo> insideServers = NowServer.getInsideServers();
                boolean match = insideServers.parallelStream().anyMatch(e -> e.getType().equals(toServerType));
                if (!match) {
                    logger.debug("serverType is not exist, toServerType:{}, forwardKey:{}!", toServerType, forwardKey);
                    return;
                }
                logger.warn("channel is null, toServerType:{}, forwardKey:{},retry 3 seconds after!", toServerType, forwardKey);
                resend(message);
            }
        }, forwardTime - now > 0 ? forwardTime - now : 0, TimeUnit.MILLISECONDS);

        //调试时放开捕获异常
        //try {
        //    final Object o = schedule.get();
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //} catch (ExecutionException e) {
        //    e.printStackTrace();
        //}
    }

    private void resend(ForwardMessage message) {
        delayThreadPoolScheduler.schedule(() -> execSend(message), 3, TimeUnit.SECONDS);
    }


    @Override
    public void forwardFormDb() {
        List<ForwardMessage> forwardMessages = loadFromDb();
        if (Objects.isNull(forwardMessages) || forwardMessages.isEmpty()) {
            return;
        }
        long now = now();
        //已过期消息删除
        forwardMessages.stream().filter(e -> {
            long expireTime = e.getExpireTime();
            return (now > expireTime) && expireTime != -1;
        })
                .map(ForwardMessage::getId)
                .collect(Collectors.toList()).forEach(this::removeFromDb);
        //未过期消息重发
        forwardMessages.stream().filter(e -> {
            long expireTime = e.getExpireTime();
            return !((now > expireTime) && expireTime != -1);
        })
                .collect(Collectors.toList()).forEach(this::execSend);
    }

    /**
     * 从数据库加载
     */
    protected abstract List<ForwardMessage> loadFromDb();

    /**
     * 保存到数据库
     */
    protected abstract void save2Db(ForwardMessage message);

    /**
     * 从数据库删除
     *
     * @param id 消息id
     */
    protected abstract void removeFromDb(Long id);

}
