package com.hbsoo.hall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by zun.wei on 2024/6/6.
 */
@ComponentScan(basePackages = {
        "com.hbsoo.server.action.mqtt",
})
@SpringBootApplication
public class HallServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HallServerApplication.class, args);
    }

}
