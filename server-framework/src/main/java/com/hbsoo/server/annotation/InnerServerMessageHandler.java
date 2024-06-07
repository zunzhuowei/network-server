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

    int value();

    /**
     * http协议专属 http请求
     */
    String uri() default "";
}
