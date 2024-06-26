package com.hbsoo.server.session;

import com.hbsoo.server.client.outer.HttpOuterUserLoginAuthenticator;
import com.hbsoo.server.client.outer.TcpOuterUserLoginAuthenticator;
import com.hbsoo.server.client.outer.UdpOuterUserLoginAuthenticator;
import com.hbsoo.server.client.outer.WebsocketOuterUserLoginAuthenticator;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.ForwardMessage;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.queue.ForwardMessageSender;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 管理外部用户session
 * Created by zun.wei on 2024/5/31.
 */
public final class OuterSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(OuterSessionManager.class);
    @Autowired
    private ForwardMessageSender forwardMessageSender;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired(required = false)
    private HttpOuterUserLoginAuthenticator httpOuterUserLoginAuthenticator;
    @Autowired(required = false)
    private TcpOuterUserLoginAuthenticator tcpOuterUserLoginAuthenticator;
    @Autowired(required = false)
    private UdpOuterUserLoginAuthenticator udpOuterUserLoginAuthenticator;
    @Autowired(required = false)
    private WebsocketOuterUserLoginAuthenticator websocketOuterUserLoginAuthenticator;
    //用户session
    static Map<Long, UserSession> clients = new ConcurrentHashMap<>();
    private final ServerInfo nowServerInfo;

    /**
     * 构造函数
     *
     * @param nowServerInfo 当前服务器信息
     */
    public OuterSessionManager(ServerInfo nowServerInfo) {
        this.nowServerInfo = nowServerInfo;
    }

    /**
     * 获取用户session
     *
     * @param id 用户id
     * @return 用户session
     */
    public UserSession getUserSession(Long id) {
        return clients.get(id);
    }

    /**
     * 获取在该服务器登录的用户session
     */
    public Map<Long, UserSession> getClients() {
        // 返回UserSession中Channel不为空的Map
        Map<Long, UserSession> result = new ConcurrentHashMap<>();
        clients.forEach((id, userSession) -> {
            if (Objects.nonNull(userSession.getChannel())) {
                result.put(id, userSession);
            }
        });
        return result;
    }

    /**
     * 登录到服务器，登录成功后，通知其他服务器
     *
     * @param id          用户id
     * @param userSession 用户session
     */
    public void loginAndSyncAllServer(Long id, UserSession userSession) {
        login(id, userSession);
        // 保存id,断线的时候踢出登录
        userSession.getChannel().attr(AttributeKeyConstants.idAttr).set(userSession.getId());
        HBSPackage.Builder builder = HBSPackage.Builder.withDefaultHeader()
                .msgType(HBSMessageType.Inner.LOGIN_SYNC)
                .writeLong(id)//登录用户id
                .writeInt(userSession.getBelongServer().getId()) //登录所属服务器id
                .writeStr(userSession.getBelongServer().getHost())
                .writeInt(userSession.getBelongServer().getPort())
                .writeStr(userSession.getBelongServer().getType());
        InnerClientSessionManager.forwardMsg2AllServerByKeyUseSender(builder, id);
    }

    /**
     * 同步登录
     *
     * @param id          用户id
     * @param userSession 用户session
     */
    public void login(Long id, UserSession userSession) {
        clients.put(id, userSession);
    }

    /**
     * 登出服务器，通知其他服务器
     *
     * @param id 用户id
     */
    public void logoutAndSyncAllServer(Long id) {
        logout(id);
        InnerClientSessionManager.forwardMsg2AllServerByKeyUseSender(
                HBSPackage.Builder.withDefaultHeader()
                        .msgType(HBSMessageType.Inner.LOGOUT_SYNC)
                        .writeLong(id),
                id
        );
    }

    /**
     * 同步登出
     *
     * @param id 用户id
     */
    public void logout(Long id) {
        clients.remove(id);
        if (httpOuterUserLoginAuthenticator != null) {
            try {
                httpOuterUserLoginAuthenticator.logoutCallback(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (tcpOuterUserLoginAuthenticator != null) {
            try {
                tcpOuterUserLoginAuthenticator.logoutCallback(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (udpOuterUserLoginAuthenticator != null) {
            try {
                udpOuterUserLoginAuthenticator.logoutCallback(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (websocketOuterUserLoginAuthenticator != null) {
            try {
                websocketOuterUserLoginAuthenticator.logoutCallback(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 退出所有指定服务器的用户session
     * @param serverType 服务器类型
     * @param serverId 服务器id
     */
    public void logoutWithBelongServer(String serverType, Integer serverId) {
        List<Long> ids = clients.values().stream()
                .filter(userSession ->
                        userSession.getBelongServer().getType().equals(serverType) &&
                        userSession.getBelongServer().getId().equals(serverId))
                .map(UserSession::getId).collect(Collectors.toList());
        ids.forEach(this::logout);
        logger.debug("退出所有指定服务器的用户session, serverType:{}, serverId:{}", serverType, serverId);
    }

    /**
     * 根据协议类型发送消息到用户
     *
     * @param protocol   用户协议类型
     * @param msgBuilder 消息
     * @param ids        用户id
     */
    public void sendMsg2User(UserSessionProtocol protocol, HBSPackage.Builder msgBuilder, Long... ids) {
        sendMsg2User(protocol, msgBuilder.buildPackage(), ids);
    }

    public void sendTextWebSocketFrameMsg2User(String text, Long... ids) {
        sendMsg2User(UserSessionProtocol.text_websocket, text.getBytes(StandardCharsets.UTF_8), ids);
    }

    /**
     * 发送消息到用户
     *
     * @param protocol     用户协议类型
     * @param innerPackage 消息, HBSPackage.Builder#buildPackage()
     * @param ids          用户id
     */
    public void sendMsg2User(UserSessionProtocol protocol, byte[] innerPackage, Long... ids) {
        if (Objects.isNull(innerPackage) || Objects.isNull(protocol)) {
            return;
        }
        for (Long id : ids) {
            try {
                UserSession userSession = clients.get(id);
                if (Objects.isNull(userSession)) {
                    logger.warn("用户id:{}未登录", id);
                    continue;
                }
                final Channel channel = userSession.getChannel();
                if (Objects.isNull(channel)) {
                    //转发到他登录的服务器中，再由登录服务器转发给用户
                    HBSPackage.Builder redirectPackage = HBSPackage.Builder
                            .withDefaultHeader()
                            .msgType(HBSMessageType.Inner.REDIRECT)
                            .writeLong(id)//用户id
                            .writeStr(protocol.name())//用户协议类型
                            .writeBytes(innerPackage);//转发的消息

                    //重试三次
                    long msgId = snowflakeIdGenerator.generateId();
                    ForwardMessage forwardMessage = new ForwardMessage(msgId, redirectPackage,
                            new Date(System.currentTimeMillis() + 1000 * 10), -1,
                            userSession.getBelongServer().getType(), null);
                    forwardMessage.setToServerId(userSession.getBelongServer().getId());
                    forwardMessageSender.send(forwardMessage);

                    //InnerClientSessionManager.sendMsg2ServerByServerTypeAndId(redirectPackage,
                    //        userSession.getBelongServer().getId(),
                    //        userSession.getBelongServer().getType());
                    continue;
                }
                if (!channel.isActive()) {
                    continue;
                }
                // 用他登录的服务器管道发送消息
                switch (protocol) {
                    case binary_websocket: {
                        BinaryWebSocketFrame socketFrame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(innerPackage));
                        channel.writeAndFlush(socketFrame);
                        break;
                    }
                    case text_websocket: {
                        TextWebSocketFrame socketFrame = new TextWebSocketFrame(new String(innerPackage, StandardCharsets.UTF_8));
                        channel.writeAndFlush(socketFrame);
                        break;
                    }
                    case tcp: {
                        channel.writeAndFlush(Unpooled.wrappedBuffer(innerPackage));
                        break;
                    }
                    case udp:
                        DatagramPacket packet = new DatagramPacket(
                                Unpooled.wrappedBuffer(innerPackage),
                                new InetSocketAddress(userSession.getUdpHost(),
                                        userSession.getUdpPort())
                        );
                        channel.writeAndFlush(packet);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                logger.error("发送消息给用户失败，id:{},e:{}", id, e);
            }
        }
    }


}
