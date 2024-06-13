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
public @interface InnerServerMessageHandler {

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
    Protocol protocol() default Protocol.TCP;
}
