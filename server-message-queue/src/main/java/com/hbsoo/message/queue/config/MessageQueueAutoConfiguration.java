package com.hbsoo.message.queue.config;

import com.hbsoo.message.queue.handlers.PublishHandler;
import com.hbsoo.message.queue.handlers.PublishPushHandler;
import com.hbsoo.message.queue.handlers.SubscribeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by zun.wei on 2024/6/26.
 */
@Import({
        RegisterSubscriptionMessage2Server.class,
        SubscribeHandler.class,
        PublishHandler.class,
        PublishPushHandler.class,
})
@Configuration
public class MessageQueueAutoConfiguration {



}
