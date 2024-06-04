package com.hbsoo.server.test;

import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.config.ServerInfoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Created by zun.wei on 2024/6/3.
 */
@SpringBootApplication
public class ServerTest {

//    @Autowired
//    ServerInfoProperties serverInfoProperties;
//    @Autowired
//    HttpMessageHandlerTest httpMessageHandlerTest;
//
//    @Bean
//    public NetworkServer networkServer() {
//        final Map<String, Object> outerServer = serverInfoProperties.getOuterServer();
//        final Object port = outerServer.get("port");
//        return new NetworkServer(Integer.parseInt(port.toString()), httpMessageHandlerTest);
//    }

    public static void main(String[] args) {
        SpringApplication.run(ServerTest.class, args);
    }

}
