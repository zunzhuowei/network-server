package com.hbsoo.server.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by zun.wei on 2024/6/5.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface OuterServerMessageHandler {

    /**
     * 要处理的消息类型
     */
    int value();

    /**
     * http协议专属 http请求
     */
    String uri() default "";

    /**
     * 要处理的协议类型
     */
    Protocol protocol() default Protocol.WEBSOCKET;

    /**
     * 接口权限, 默认为普通用户；
     * 判断客户端（http，udp）请求中的token是否合法或者(TCP、Websocket)channel中是否
     * 包含接口权限字段。校验方式请参考server-permission：PermissionAspect.java
     */
    Permission[] permission() default {Permission.USER};

    /**
     * 接口权限,权限字符串;优先判断字符串再判断枚举类
     * {@link OuterServerMessageHandler#permission}
     */
    String[] permissionStr() default {};

}
