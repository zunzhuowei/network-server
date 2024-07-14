package com.hbsoo.server.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.ForwardMessage;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.sender.ForwardMessageSender;
import com.hbsoo.server.netty.AttributeKeyConstants;
import com.hbsoo.server.netty.ProtocolDispatcher;
import com.hbsoo.server.utils.SnowflakeIdGenerator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 管理外部用户session
 * Created by zun.wei on 2024/5/31.
 */
public final class OutsideUserSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(OutsideUserSessionManager.class);
    @Autowired
    private ForwardMessageSender forwardMessageSender;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    //用户session
    static Map<Long, UserSession> clients = new ConcurrentHashMap<>();
    // udp host:port与id的映射
    static Map<String, Long> udpSenderRelation = new ConcurrentHashMap<>();
    private final ServerInfo nowServerInfo;

    /**
     * 构造函数
     *
     * @param nowServerInfo 当前服务器信息
     */
    public OutsideUserSessionManager(ServerInfo nowServerInfo) {
        this.nowServerInfo = nowServerInfo;
    }

    public ServerInfo getNowServerInfo() {
        return this.nowServerInfo;
    }

    /**
     * 获取upd sender映射的用户id
     * @param senderHost 发送端地址
     * @param senderPort 发送端端口
     * @return 用户id
     */
    public Long getUdpRelativeUserId(String senderHost, int senderPort) {
        return udpSenderRelation.get(senderHost + ":" + senderPort);
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
    private void loginAndSyncAllServer(Long id, UserSession userSession) {
        login(id, userSession);
        if (!(userSession.getOutsideUserProtocol() == OutsideUserProtocol.UDP)) {
            // 保存id,断线的时候踢出登录
            userSession.getChannel().attr(AttributeKeyConstants.idAttr).set(userSession.getId());
        }
        Gson gson = new Gson();
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(MessageType.Inside.LOGIN_SYNC)
                .writeLong(id)//登录用户id
                .writeInt(userSession.getBelongServer().getId()) //登录所属服务器id
                .writeStr(userSession.getBelongServer().getHost())
                .writeInt(userSession.getBelongServer().getPort())
                .writeStr(userSession.getBelongServer().getType())
                .writeStr(gson.toJson(userSession.getPermissions()))
                .writeStr(userSession.getChannelId())
                .writeByte(userSession.getProtocolType())
                ;
        if (userSession.getOutsideUserProtocol() == OutsideUserProtocol.UDP) {
            builder.writeStr(userSession.getUdpHost()).writeInt(userSession.getUdpPort());
        }
        InsideClientSessionManager.forwardMsg2AllServerByKeyUseSender(builder, id);
    }

    private void loginSyncAllServer(Channel channel, Long userId, byte protocolType, String... permissions) {
        UserSession userSession = new UserSession();
        userSession.setId(userId);
        userSession.setBelongServer(nowServerInfo);
        userSession.setChannel(channel);
        userSession.setProtocolType(protocolType);
        for (String permission : permissions) {
            userSession.addPermission(permission);
        }
        loginAndSyncAllServer(userId, userSession);
    }

    public void loginWithTcpAndSyncAllServer(Channel channel, Long userId, String... permissions) {
        loginSyncAllServer(channel, userId, OutsideUserProtocol.TCP.protocolType, permissions);
    }

    public void loginWithBinWebsocketAndSyncAllServer(Channel channel, Long userId, String... permissions) {
        loginSyncAllServer(channel, userId, OutsideUserProtocol.BINARY_WEBSOCKET.protocolType, permissions);
    }
    public void loginWithTxtWebsocketAndSyncAllServer(Channel channel, Long userId, String... permissions) {
        loginSyncAllServer(channel, userId, OutsideUserProtocol.TEXT_WEBSOCKET.protocolType, permissions);
    }

    public void loginWithHttpAndSyncAllServer(Channel channel, Long userId, String... permissions) {
        loginSyncAllServer(channel, userId, OutsideUserProtocol.HTTP.protocolType, permissions);
    }

    public void loginWithUdpAndSyncAllServer(Channel channel, Long userId, String senderHost, int senderPort, String... permissions) {
        UserSession userSession = new UserSession();
        userSession.setId(userId);
        userSession.setBelongServer(nowServerInfo);
        userSession.setChannel(channel);
        userSession.setProtocolType(OutsideUserProtocol.UDP.protocolType);
        userSession.setUdpHost(senderHost);
        userSession.setUdpPort(senderPort);
        for (String permission : permissions) {
            userSession.addPermission(permission);
        }
        logout(userId);
        loginAndSyncAllServer(userId, userSession);
        udpSenderRelation.putIfAbsent(senderHost + ":" + senderPort, userId);
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
        InsideClientSessionManager.forwardMsg2AllServerByKeyUseSender(
                NetworkPacket.Builder.withDefaultHeader()
                        .msgType(MessageType.Inside.LOGOUT_SYNC)
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
        udpSenderRelation.forEach((k, v) -> {
            if (v.equals(id)) {
                udpSenderRelation.remove(k);
            }
        });
    }

    /**
     * 退出所有指定服务器的用户session
     *
     * @param serverType 服务器类型
     * @param serverId   服务器id
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
    public void sendMsg2User(OutsideUserProtocol protocol, NetworkPacket.Builder msgBuilder, Long... ids) {
        sendMsg2User(protocol, msgBuilder.buildPackage(), ids);
    }

    public void sendTextWebSocketFrameMsg2User(String text, Long... ids) {
        sendMsg2User(OutsideUserProtocol.TEXT_WEBSOCKET, text.getBytes(StandardCharsets.UTF_8), ids);
    }

    /**
     * 发送消息到用户
     *
     * @param protocol     用户协议类型
     * @param insidePackage 消息, HBSPackage.Builder#buildPackage()
     * @param ids          用户id
     */
    public void sendMsg2User(OutsideUserProtocol protocol, byte[] insidePackage, Long... ids) {
        if (Objects.isNull(insidePackage) || Objects.isNull(protocol)) {
            return;
        }
        if (protocol == OutsideUserProtocol.HTTP) {
            logger.warn("http协议不能此方法发送消息到用户，请用httpResponse()");
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
                    NetworkPacket.Builder redirectPackage = NetworkPacket.Builder
                            .withDefaultHeader()
                            .msgType(MessageType.Inside.REDIRECT)
                            .writeStr(protocol.name())//用户协议类型
                            .writeLong(id)//用户id
                            .writeBytes(insidePackage);//转发的消息

                    //重试三次
                    long msgId = snowflakeIdGenerator.generateId();
                    ForwardMessage forwardMessage = new ForwardMessage(msgId, redirectPackage,
                            new Date(System.currentTimeMillis() + 1000 * 10), -1,
                            userSession.getBelongServer().getType(), null);
                    forwardMessage.setToServerId(userSession.getBelongServer().getId());
                    forwardMessageSender.send(forwardMessage);
                    continue;
                }
                if (!channel.isActive()) {
                    continue;
                }
                // 用他登录的服务器管道发送消息
                switch (protocol) {
                    case BINARY_WEBSOCKET: {
                        BinaryWebSocketFrame socketFrame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(insidePackage));
                        channel.writeAndFlush(socketFrame);
                        break;
                    }
                    case TEXT_WEBSOCKET: {
                        TextWebSocketFrame socketFrame = new TextWebSocketFrame(new String(insidePackage, StandardCharsets.UTF_8));
                        channel.writeAndFlush(socketFrame);
                        break;
                    }
                    case TCP: {
                        channel.writeAndFlush(Unpooled.wrappedBuffer(insidePackage));
                        break;
                    }
                    case UDP: {
                        DatagramPacket packet = new DatagramPacket(
                                Unpooled.wrappedBuffer(insidePackage),
                                new InetSocketAddress(userSession.getUdpHost(),
                                        userSession.getUdpPort())
                        );
                        channel.writeAndFlush(packet);
                        break;
                    }
                    default:
                        break;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                logger.error("发送消息给用户失败，id:{},e:{}", id, e);
            }
        }
    }

    /**
     * @param content 消息内容
     * @param contentType  内容类型，针对http协议使用。
     * @param extendBody 扩展信息
     */
    public void httpResponse(Map<String, String> headers, byte[] content, String contentType, ExtendBody extendBody, HttpResponseStatus status,
                             GenericFutureListener<? extends Future<? super Void>> future) {
        String fromServerType = extendBody.getFromServerType();
        int fromServerId = extendBody.getFromServerId();
        String userChannelId = extendBody.getUserChannelId();
        //如果是本服务器，则直接返回
        if (NowServer.getServerInfo().getType().equals(fromServerType)
                && fromServerId == NowServer.getServerInfo().getId()) {
            ChannelManager.getChannel(userChannelId)
                    .ifPresent(channel -> response(channel, headers, content, contentType, status, future));
            return;
        }
        //转发到他登录的服务器中，再由登录服务器转发给用户
        NetworkPacket.Builder redirectPackage = NetworkPacket.Builder
                .withDefaultHeader()
                .msgType(MessageType.Inside.REDIRECT)
                .writeStr(OutsideUserProtocol.HTTP.name())//用户协议类型
                .writeStr(contentType)
                .writeInt(status.code())
                .writeBytes(content == null ? new byte[0] : content)
                .writeStr(new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                        .create()
                        .toJson(headers == null ? new HashMap<String, String>() : headers)
                )
                .writeExtendBodyMode()
                .writeObj(extendBody);

        //重试三次
        long msgId = snowflakeIdGenerator.generateId();
        ForwardMessage forwardMessage = new ForwardMessage(msgId, redirectPackage,
                new Date(System.currentTimeMillis() + 1000 * 10), -1,
                fromServerType, null);
        forwardMessage.setToServerId(fromServerId);
        forwardMessageSender.send(forwardMessage);
    }
    public void httpResponse(Map<String, String> headers, byte[] content, String contentType, ExtendBody extendBody,
                             GenericFutureListener<? extends Future<? super Void>> future) {
        httpResponse(headers, content, contentType, extendBody, HttpResponseStatus.OK, future);
    }
    public void httpResponse(Map<String, String> headers, byte[] content, String contentType, ExtendBody extendBody) {
        httpResponse(headers, content, contentType, extendBody, HttpResponseStatus.OK, null);
    }
    public void httpResponse(Map<String, String> headers, byte[] content, String contentType, ExtendBody extendBody, HttpResponseStatus status) {
        httpResponse(headers, content, contentType, extendBody, status, null);
    }

    private void response(Channel channel, Map<String, String> headers, byte[] content, String contentType, HttpResponseStatus status,
                          GenericFutureListener<? extends Future<? super Void>> future) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        if (content != null && content.length > 0) {
            response.content().writeBytes(content);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (Objects.nonNull(headers)) {
            headers.forEach((k, v) -> response.headers().set(k, v));
        }
        if (Objects.nonNull(future)) {
            channel.writeAndFlush(response).addListener(future).addListener(ChannelFutureListener.CLOSE);
        } else {
            channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
