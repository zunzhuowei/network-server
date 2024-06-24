package com.hbsoo.permisson;

import com.google.gson.Gson;
import com.hbsoo.permisson.utils.JwtUtils;
import com.hbsoo.server.annotation.OuterServerMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.HBSMessageType;
import com.hbsoo.server.message.entity.HBSPackage;
import com.hbsoo.server.message.entity.HttpPackage;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import com.hbsoo.server.netty.AttributeKeyConstants;
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

    @Pointcut("@annotation(com.hbsoo.server.annotation.OuterServerMessageHandler)"
            + "|| @within(com.hbsoo.server.annotation.OuterServerMessageHandler)"
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
        final Object[] args = point.getArgs();
        OuterServerMessageHandler outerServerMessageHandler = getOuterServerMessageHandler(point);
        if (Objects.nonNull(outerServerMessageHandler)) {
            String[] permissionStr = outerServerMessageHandler.permissionStr();
            Permission[] permission = outerServerMessageHandler.permission();
            Protocol protocol = outerServerMessageHandler.protocol();
            if (checkPermission(point, args, permissionStr, permission, protocol)) {
                return point.proceed();
            } else {
                logger.debug("权限不足");
            }
        }
        return null;
    }

    /**
     * 检查权限
     */
    private boolean checkPermission(ProceedingJoinPoint point, Object[] args,
                                    String[] permissionStr, Permission[] permission,
                                    Protocol protocol) throws Throwable {
        Set<String> permissions = Stream.concat(
                Arrays.stream(permissionStr).map(String::toUpperCase),
                Arrays.stream(permission).map(Permission::name)
        ).collect(Collectors.toSet());
        ChannelHandlerContext context = Objects.isNull(args[0]) ? null : (ChannelHandlerContext) args[0];
        //http
        if (protocol == Protocol.HTTP) {
            HttpPackage httpPackage = Objects.isNull(args[1]) ? null : (HttpPackage) args[1];
            if (Objects.isNull(httpPackage)) {
                return true;
            }
            String authentication = httpPackage.getHeaders().get("Authentication");
            if (checkJwtPermission(permissions, authentication)) return true;
            ((HttpServerMessageDispatcher) point.getTarget())
                    .responseHtml(context, "<h1>权限不足</h1>", null);
            return false;
        }
        //UDP特殊处理
        else if (protocol == Protocol.UDP) {
            HBSPackage.Decoder decoder = (HBSPackage.Decoder) args[1];
            String sendHost = decoder.skipGetStr(HBSPackage.DecodeSkip.INT);
            int sendPort = decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.STRING);
            String authentication = decoder.skipGetStr(
                    HBSPackage.DecodeSkip.INT,
                    HBSPackage.DecodeSkip.STRING,
                    HBSPackage.DecodeSkip.INT);
            if (checkJwtPermission(permissions, authentication)) return true;
            HBSPackage.Builder.withHeader(HBSPackage.UDP_HEADER)
                    .msgType(HBSMessageType.Outer.PERMISSION_DENIED)
                    .buildAndSendUdpTo(context.channel(), sendHost, sendPort);
            return false;
        }
        // 非http
        else {
            //HBSPackage.Decoder decoder = (HBSPackage.Decoder) args[1];
            //int msgType = decoder.getMsgType();
            if (Objects.isNull(context)) {
                return true;
            }
            String[] channelPermissions = AttributeKeyConstants.getAttr(context.channel(),
                    AttributeKeyConstants.permissionAttr);
            if (Objects.isNull(channelPermissions) || channelPermissions.length == 0) {
                if (permissions.isEmpty()) {
                    return true;
                }
            }
            if (Objects.nonNull(channelPermissions)) {
                Set<String> stringSet = Arrays.stream(channelPermissions)
                        .map(String::toUpperCase).collect(Collectors.toSet());
                for (String p : permissions) {
                    if (stringSet.contains(p)) {
                        return true;
                    }
                }
            }
        }
        if (protocol == Protocol.TCP) {
            HBSPackage.Builder.withDefaultHeader()
                    .msgType(HBSMessageType.Outer.PERMISSION_DENIED)
                    .buildAndSendBytesTo(context.channel());
        }
        if (protocol == Protocol.WEBSOCKET) {
            HBSPackage.Builder.withDefaultHeader()
                    .msgType(HBSMessageType.Outer.PERMISSION_DENIED)
                    .buildAndSendBinWebSocketTo(context.channel());
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

    public OuterServerMessageHandler getOuterServerMessageHandler(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        OuterServerMessageHandler dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), OuterServerMessageHandler.class);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), OuterServerMessageHandler.class);
    }

}
