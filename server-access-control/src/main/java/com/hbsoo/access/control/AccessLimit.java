package com.hbsoo.access.control;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessLimit {
    /**
     * 全局限流,每秒访问次数
     */
    int globalRateSize() default 500;
    /**
     * 用户限流,每秒访问次数
     */
    int userRateSize() default 5;


}
