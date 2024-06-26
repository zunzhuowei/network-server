package com.hbsoo.permisson;

/**
 * Created by zun.wei on 2024/6/26.
 */

import com.hbsoo.server.annotation.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限认证注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionAuth {

    /**
     * 接口权限, 默认为普通用户；
     * 判断客户端（http，udp）请求中的token是否合法或者(TCP、Websocket)channel中是否
     * 包含接口权限字段。校验方式请参考server-permission：PermissionAspect.java
     */
    Permission[] permission() default {Permission.USER};

    /**
     * 接口权限,权限字符串;优先判断字符串再判断枚举类
     * {@link PermissionAuth#permission}
     */
    String[] permissionStr() default {};

}
