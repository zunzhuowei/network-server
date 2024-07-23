package com.hbsoo.hall;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by zun.wei on 2024/7/23.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.hbsoo.server.action.mqtt",
})
public class MqttConfiguration {



}
