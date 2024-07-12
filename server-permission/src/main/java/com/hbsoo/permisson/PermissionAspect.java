package com.hbsoo.permisson;

import com.google.gson.Gson;
import com.hbsoo.permisson.utils.JwtUtils;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.ExpandBody;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by zun.wei on 2024/6/23.
 */
@Aspect
public class PermissionAspect {
    Logger logger = LoggerFactory.getLogger(PermissionAspect.class);

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Pointcut("@annotation(com.hbsoo.server.annotation.OutsideMessageHandler)"
            + "|| @within(com.hbsoo.server.annotation.OutsideMessageHandler)"
            //+ "|| @annotation(com.hbsoo.server.annotation.InnerServerMessageHandler)"
            //+ "|| @within(com.hbsoo.server.annotation.InnerServerMessageHandler)"
    )
    public void pointCut() {
    }


    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String methodName = point.getSignature().getName();
        if (!"handle".equals(methodName)) {
            return point.proceed();
        }
        OutsideMessageHandler outsideMessageHandler = getOuterServerMessageHandler(point);
        if (Objects.isNull(outsideMessageHandler)) {
            return point.proceed();
        }
        PermissionAuth permissionAuth = getPermissionAuth(point);
        if (Objects.isNull(permissionAuth)) {
            return point.proceed();
        }
        Object[] args = point.getArgs();
        String[] permissionStr = permissionAuth.permissionStr();
        Permission[] permission = permissionAuth.permission();
        Protocol protocol = outsideMessageHandler.protocol();
        if (checkPermission(point, args, permissionStr, permission, protocol)) {
            return point.proceed();
        } else {
            logger.debug("权限不足");
        }
        return null;
    }

    /**
     * 检查权限,
     * @return true, 有权限
     */
    private boolean checkPermission(ProceedingJoinPoint point, Object[] args,
                                    String[] permissionStr, Permission[] permission,
                                    Protocol protocol) throws Throwable {
        Set<String> permissions = Stream.concat(
                Arrays.stream(permissionStr).map(String::toUpperCase),
                Arrays.stream(permission).map(Permission::name)
        ).collect(Collectors.toSet());
        // 无需任何权限
        if (permissions.isEmpty()) {
            return true;
        }

        ChannelHandlerContext context = Objects.isNull(args[0]) ? null : (ChannelHandlerContext) args[0];
        //http
        if (protocol == Protocol.HTTP) {
            HttpPacket httpPacket = Objects.isNull(args[1]) ? null : (HttpPacket) args[1];
            if (Objects.isNull(httpPacket)) {
                return true;
            }
            String authentication = httpPacket.getHeaders().get("Authentication");
            if (checkJwtPermission(permissions, authentication)) return true;
            ((HttpServerMessageDispatcher) point.getTarget())
                    .responseHtml(context, httpPacket, "<h1>权限不足</h1>");
            return false;
        }
        //UDP特殊处理
        else if (protocol == Protocol.UDP) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
            String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
            int sendPort = decoder.skipGetInt(NetworkPacket.DecodeSkip.INT, NetworkPacket.DecodeSkip.STRING);
            String authentication = decoder.skipGetStr(
                    NetworkPacket.DecodeSkip.INT,//消息类型
                    NetworkPacket.DecodeSkip.STRING,//发送端
                    NetworkPacket.DecodeSkip.INT);//发送端口
            if (checkJwtPermission(permissions, authentication)) return true;
            NetworkPacket.Builder.withHeader(NetworkPacket.UDP_HEADER)
                    .msgType(MessageType.Outside.PERMISSION_DENIED)
                    .sendUdpTo(context.channel(), sendHost, sendPort);
            return false;
        }
        // 非http,非udp
        else {
            //int msgType = decoder.getMsgType();
            if (Objects.isNull(context)) {
                return true;
            }
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
            ExpandBody expandBody = decoder.readExpandBody();
            if (expandBody.isLogin()) {
                UserSession userSession = expandBody.getUserSession();
                if (Objects.isNull(userSession) || userSession.getId() == 0L) {
                    logger.debug("ChannelId:{}未登录，无法获取session信息", userSession.getChannelId());
                    return false;
                }
                // 重置读取位置
                decoder.resetBodyReadOffset();
                Set<String> userSessionPermissions = userSession.getPermissions();
                for (String p : permissions) {
                    if (userSessionPermissions.contains(p)) {
                        return true;
                    }
                }
            } else {
                logger.debug("未登录，无法获取session信息，{}", expandBody);
            }
        }
        if (protocol == Protocol.TCP) {
            NetworkPacket.Builder.withDefaultHeader()
                    .msgType(MessageType.Outside.PERMISSION_DENIED)
                    .sendTcpTo(context.channel());
        }
        if (protocol == Protocol.WEBSOCKET) {
            NetworkPacket.Builder.withDefaultHeader()
                    .msgType(MessageType.Outside.PERMISSION_DENIED)
                    .sendBinWebSocketTo(context.channel());
        }
        return false;
    }

    /**
     * 检查jwt权限
     * @param permissions 权限
     * @param authentication jwt
     * @return 返回true：有权限，返回false：无权限
     */
    private boolean checkJwtPermission(Set<String> permissions, String authentication) {
        if (!StringUtils.hasLength(authentication)) {
            if (permissions.isEmpty()) {
                return true;
            }
        } else {
            Claims claims = jwtUtils.getClaimByToken(authentication);
            if (Objects.nonNull(claims)) {
                Object param = claims.get("param");
                Gson gson = new Gson();
                Map map = gson.fromJson(param.toString(), Map.class);
                Object jwtPermissions = map.get("permission");
                if (Objects.nonNull(jwtPermissions)) {
                    String[] jwtPermission = jwtPermissions.toString().split(",");
                    Set<String> jwtPermissionsSet = Arrays.stream(jwtPermission)
                            .map(String::toUpperCase).collect(Collectors.toSet());
                    for (String p : permissions) {
                        if (jwtPermissionsSet.contains(p)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public OutsideMessageHandler getOuterServerMessageHandler(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        OutsideMessageHandler dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), OutsideMessageHandler.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), OutsideMessageHandler.class);
    }


    public PermissionAuth getPermissionAuth(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        PermissionAuth dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), PermissionAuth.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), PermissionAuth.class);
    }

}
