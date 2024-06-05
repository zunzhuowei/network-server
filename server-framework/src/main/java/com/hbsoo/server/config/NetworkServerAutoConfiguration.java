package com.hbsoo.server.config;

import com.hbsoo.server.NetworkClient;
import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.message.client.InnerClientMessageHandler;
import com.hbsoo.server.message.server.inner.InnerServerMessageHandler;
import com.hbsoo.server.message.server.outer.OuterServerMessageHandler;
import com.hbsoo.server.session.ServerType;
import com.hbsoo.server.utils.SpringBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by zun.wei on 2020/4/29.
 */
@Configuration
@EnableConfigurationProperties(ServerInfoProperties.class)
public class NetworkServerAutoConfiguration {

    @Autowired
    private ServerInfoProperties serverInfoProperties;


    @Bean
    public SpringBeanFactory springBeanFactory() {
        return new SpringBeanFactory();
    }

    /**
     * 暴露给外网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "hbsoo.server.outerServer", name = "enable", havingValue = "true")
    public NetworkServer outerServer() {
        final Map<String, Object> outerServer = serverInfoProperties.getOuterServer();
        final Object port = outerServer.get("port");
        final Map<String, OuterServerMessageHandler> messageHandler = springBeanFactory().getBeansOfType(OuterServerMessageHandler.class);
        OuterServerMessageHandler[] handlers = messageHandler.values().toArray(new OuterServerMessageHandler[0]);
        return new NetworkServer(Integer.parseInt(port.toString()), handlers);
    }

    /**
     * 暴漏给内网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public NetworkServer innerServer() {
        final List<ServerInfo> innerServers = serverInfoProperties.getInnerServers();
        final Integer id = serverInfoProperties.getId();
        final Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        final ServerInfo serverInfo = optional.get();
        final int port = serverInfo.getPort();
        final Map<String, InnerServerMessageHandler> messageHandler = springBeanFactory().getBeansOfType(InnerServerMessageHandler.class);
        InnerServerMessageHandler[] handlers = messageHandler.values().toArray(new InnerServerMessageHandler[0]);
        return new NetworkServer(port, handlers);
    }

    /**
     * 内部服务沟通客户端
     */
    @Bean(initMethod = "connect", destroyMethod = "stop")
    public NetworkClient networkClient() {
        final Integer id = serverInfoProperties.getId();
        final List<ServerInfo> innerServers = serverInfoProperties.getInnerServers();
        final Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        final ServerInfo serverInfo = optional.get();
        final ServerType type = serverInfo.getType();
        final Map<String, InnerClientMessageHandler> messageHandler = springBeanFactory().getBeansOfType(InnerClientMessageHandler.class);
        InnerClientMessageHandler[] handlers = messageHandler.values().toArray(new InnerClientMessageHandler[0]);
        //TcpClientMessageHandler clientMessageHandler = springBeanFactory().getBean(TcpClientMessageHandler.class);
        return new NetworkClient(innerServers, handlers, id, type);
    }


}
