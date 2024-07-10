package com.hbsoo.access.control;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.hbsoo.server.message.MessageType;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import com.hbsoo.server.netty.AttributeKeyConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Aspect
public final class AccessLimitAspect {

    @Autowired
    private SystemRateLimiter systemRateLimiter;
    @Autowired
    private AccessControlProperties accessControlProperties;
    Logger logger = LoggerFactory.getLogger(AccessLimitAspect.class);
    static final Map<Class<?>, Object[]> systemRateLimitersMap = new ConcurrentHashMap<>();

    Object[] getSystemRateLimiters(Class<?> actionClass, AccessLimit accessLimit) {
        return systemRateLimitersMap.computeIfAbsent(actionClass, key -> {
            Object[] rms = new Object[2];
            rms[0] = RateLimiter.create(accessLimit.globalRateSize());
            rms[1] = CacheBuilder.newBuilder()
                    .expireAfterWrite(3, TimeUnit.SECONDS) // 用户限流器三秒钟后过期
                    .build(new CacheLoader<Long, RateLimiter>() {
                        @Override
                        public RateLimiter load(Long userId) throws Exception {
                            //System.out.println("userId = " + userId);
                            return RateLimiter.create(accessLimit.userRateSize());
                        }
                    });
            return rms;
        });
    }

    @Pointcut("@annotation(com.hbsoo.server.annotation.OutsideMessageHandler)"
            + "|| @within(com.hbsoo.server.annotation.OutsideMessageHandler)"
            //+ "|| @annotation(com.hbsoo.server.annotation.InnerServerMessageHandler)"
            //+ "|| @within(com.hbsoo.server.annotation.InnerServerMessageHandler)"
    )
    public void pointCut() {
    }

    //@Around("@annotation(accessLimit)")
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        if (!"handle".equals(methodName)) {
            return joinPoint.proceed();
        }

        Object target = joinPoint.getTarget();
        Class<?> aClass = target.getClass();
        Object[] args = joinPoint.getArgs();
        ChannelHandlerContext context = Objects.isNull(args[0]) ? null : (ChannelHandlerContext) args[0];

        //判断ip黑白名单
        List<String> blockIpList = accessControlProperties.getBlockIpList();
        List<String> whiteIpList = accessControlProperties.getWhiteIpList();
        if (context.channel() instanceof NioDatagramChannel) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
            String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
            //int sendPort = decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.STRING);
            if (Objects.nonNull(blockIpList) && blockIpList.contains(sendHost)) {
                logger.debug("Block ip {}", sendHost);
                responseAccessLimit(target, args, context);
                return null;
            }
            if (Objects.nonNull(whiteIpList) && whiteIpList.contains(sendHost)) {
                logger.debug("White ip {}", sendHost);
                return joinPoint.proceed();
            }
        } else {
            SocketAddress socketAddress = context.channel().remoteAddress();
            String hostString = ((InetSocketAddress) socketAddress).getHostString();
            if (Objects.nonNull(blockIpList) && blockIpList.contains(hostString)) {
                logger.debug("Block ip {}", hostString);
                responseAccessLimit(target, args, context);
                return null;
            }
            if (Objects.nonNull(whiteIpList) && whiteIpList.contains(hostString)) {
                logger.debug("White ip {}", hostString);
                return joinPoint.proceed();
            }
        }
        //系统配置文件限流配置
        AccessLimit accessLimit = getAccessLimit(joinPoint);
        if (accessLimit == null) {
            boolean globalAcquire = systemRateLimiter.tryGlobalAcquire();
            if (!globalAcquire) {
                logger.debug("Global rate limit");
                responseAccessLimit(target, args, context);
                return null;
            }
            Long userId = null;
            //UDP
            if (context.channel() instanceof NioDatagramChannel) {
                NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
                String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
                //int sendPort = decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.STRING);
                userId = (long) Math.abs(sendHost.hashCode());
            } else {
                // TCP,WEBSOCKET
                userId = AttributeKeyConstants.getAttr(context.channel(), AttributeKeyConstants.idAttr);
                // HTTP or no login
                if (Objects.isNull(userId)) {
                    SocketAddress socketAddress = context.channel().remoteAddress();
                    String hostString = ((InetSocketAddress) socketAddress).getHostString();
                    userId = (long) Math.abs(hostString.hashCode());
                }
            }
            boolean userAcquire = systemRateLimiter.tryUserAcquire(userId);
            if (!userAcquire) {
                logger.debug("User rate limit");
                responseAccessLimit(target, args, context);
                return null;
            }
            return joinPoint.proceed();
        }

        //注解限流配置

        Object[] systemRateLimiters = getSystemRateLimiters(aClass, accessLimit);
        RateLimiter globalRateLimiter = (RateLimiter) systemRateLimiters[0];
        if (!globalRateLimiter.tryAcquire()) {
            logger.debug("AccessLimit Global rate limit");
            responseAccessLimit(target, args, context);
            return null;
        }
        LoadingCache<Long, RateLimiter> userRateLimiters = (LoadingCache<Long, RateLimiter>) systemRateLimiters[1];
        Long userId = null;
        //UDP
        if (context.channel() instanceof NioDatagramChannel) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
            String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
            //int sendPort = decoder.skipGetInt(HBSPackage.DecodeSkip.INT, HBSPackage.DecodeSkip.STRING);
            userId = (long) Math.abs(sendHost.hashCode());
        } else {
            // TCP,WEBSOCKET
            userId = AttributeKeyConstants.getAttr(context.channel(), AttributeKeyConstants.idAttr);
            // HTTP or no login
            if (Objects.isNull(userId)) {
                SocketAddress socketAddress = context.channel().remoteAddress();
                String hostString = ((InetSocketAddress) socketAddress).getHostString();
                userId = (long) Math.abs(hostString.hashCode());
            }
        }
        if (!userRateLimiters.get(userId).tryAcquire()) {
            logger.debug("AccessLimit User rate limit");
            responseAccessLimit(target, args, context);
            return null;
        }
        return joinPoint.proceed();
    }

    /**
     * 访问限制
     * @param target 切面对象
     * @param args 切点参数
     * @param context 通道
     */
    private void responseAccessLimit(Object target, Object[] args, ChannelHandlerContext context) {
        if (context.channel() instanceof NioDatagramChannel) {
            NetworkPacket.Decoder decoder = (NetworkPacket.Decoder) args[1];
            String sendHost = decoder.skipGetStr(NetworkPacket.DecodeSkip.INT);
            int sendPort = decoder.skipGetInt(NetworkPacket.DecodeSkip.INT, NetworkPacket.DecodeSkip.STRING);
            NetworkPacket.Builder.withHeader(NetworkPacket.UDP_HEADER)
                    .msgType(MessageType.Outside.TOO_MANY_REQUESTS)
                    .sendUdpTo(context.channel(), sendHost, sendPort);
        } else if (target instanceof HttpServerMessageDispatcher) {
            HttpPacket httpPacket = (HttpPacket) args[1];
            ((HttpServerMessageDispatcher) target)
                    .responseHtml(context, httpPacket, "<h1>429</h1>");
        } else {
            NetworkPacket.Builder.withDefaultHeader()
                    .msgType(MessageType.Outside.TOO_MANY_REQUESTS)
                    .sendBinWebSocketTo(context.channel());
            //HBSPackage.Builder.withDefaultHeader()
            //        .msgType(HBSMessageType.Outer.TOO_MANY_REQUESTS)
            //        .buildAndSendBytesTo(context.channel());
        }
    }


    public AccessLimit getAccessLimit(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), AccessLimit.class);
    }

}
