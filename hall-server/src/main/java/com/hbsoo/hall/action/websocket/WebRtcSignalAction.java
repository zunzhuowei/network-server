package com.hbsoo.hall.action.websocket;

import com.google.gson.Gson;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.message.entity.ExtendBody;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zun.wei on 2024/7/24.
 */
public class WebRtcSignalAction {


    @PermissionAuth
    @OutsideMessageHandler(999)
    public static class WebRtcHeartBeatAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            UserSession userSession = extendBody.getUserSession();
            Long userId = userSession.getId();
            logger.debug("收到心跳消息:{}", userId);
            /*outsideUserSessionManager.sendMsg2User(
                    OutsideUserProtocol.TEXT_WEBSOCKET,
                    decoder.toBuilder(),
                    userId
            );*/
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @OutsideMessageHandler(990)
    public static class WebRtcLoginAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");
            String username = data.get("username").toString();
            int userId = Math.abs(username.hashCode());
            logger.info("login chat room username:{}，channelId:{}，userId:{}", username, channelId, userId);
            //通知客户端登录成功
            NetworkPacket.Builder builder = decoder.toBuilder().msgType(100).writeInt(userId).writeStr(Permission.USER.name());
            builder.sendTcpTo(ctx.channel());
            logger.debug("收到登录消息:{}", json);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(994)
    public static class CallStartAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();

            long userId = (long)Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",fromUser);
            map.put("msg","1");
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(993)
    public static class HangupAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();

            long userId = (long)Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",fromUser);
            map.put("msg","对方挂断！");
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(995)
    public static class IceAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();
            Map iceCandidate = (Map) data.get("iceCandidate");

            long userId = Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",toUser);
            map.put("iceCandidate",iceCandidate);
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(997)
    public static class OfferAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();
            String sdp = data.get("sdp").toString();

            long userId = Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",toUser);
            map.put("sdp",sdp);
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(996)
    public static class AnswerAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();
            String sdp = data.get("sdp").toString();

            long userId = Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",toUser);
            map.put("sdp",sdp);
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

    @PermissionAuth
    @OutsideMessageHandler(998)
    public static class CallbackAction extends ServerMessageDispatcher {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        @Autowired
        private OutsideUserSessionManager outsideUserSessionManager;

        @Override
        public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            ExtendBody extendBody = decoder.readExtendBody();
            String channelId = extendBody.getUserChannelId();
            final String json = decoder.readStr();
            Gson gson = new Gson();
            Map info = gson.fromJson(json, Map.class);
            Map data = (Map) info.get("data");

            String type = data.get("type").toString();
            String toUser = data.get("toUser").toString();
            String fromUser = data.get("fromUser").toString();
            String msg = data.get("msg").toString();

            long userId = Math.abs(toUser.hashCode());
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",type);
            map.put("fromUser",toUser);
            map.put("msg",msg);
            outsideUserSessionManager.sendTextWebSocketMsg2User(gson.toJson(map), userId);
        }

        @Override
        public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
            return null;
        }
    }

}
