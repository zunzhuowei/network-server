package com.hbsoo.message.queue.config;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zun.wei on 2024/6/26.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface MessageListener {

    /**
     * 监听的主题名称
     */
    String topic();

    /**
     * 定义为消息队列服务类型名称
     */
    String serverType();


}
