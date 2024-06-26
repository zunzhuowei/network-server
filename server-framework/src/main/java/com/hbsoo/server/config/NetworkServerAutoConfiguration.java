package com.hbsoo.server.config;

import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.client.DefaultInnerTcpClientConnectListener;
import com.hbsoo.server.message.client.InnerClientMessageDispatcher;
import com.hbsoo.server.message.queue.DefaultForwardMessageSender;
import com.hbsoo.server.message.queue.ForwardMessageSender;
import com.hbsoo.server.message.server.InnerServerMessageDispatcher;
import com.hbsoo.server.message.server.OuterServerMessageDispatcher;
import com.hbsoo.server.session.OuterSessionManager;
import com.hbsoo.server.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.*;

/**
 * Created by zun.wei on 2020/4/29.
 */
@ComponentScan(basePackages = {
        //"com.hbsoo.server.actiontest",
        "com.hbsoo.server.action.client",
        "com.hbsoo.server.action.server",
})
@Import({
        SpringBeanDefinitionRegistrar.class,
        InnerClientMessageDispatcher.class,
        InnerServerMessageDispatcher.class,
        OuterServerMessageDispatcher.class,
        DefaultInnerTcpClientConnectListener.class,
})
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
        Map<String, Object> outerServer = serverInfoProperties.getOuterServer();
        Object port = outerServer.get("port");
        String protocolsStr = Objects.isNull(outerServer.get("protocol"))
                ? "TCP,UDP,WEBSOCKET,HTTP" : outerServer.get("protocol").toString();
        HashSet<String> protocols = new HashSet<>(Arrays.asList(protocolsStr.split(",")));
        OuterServerMessageDispatcher handler = SpringBeanFactory.getBean(OuterServerMessageDispatcher.class);
        return new NetworkServer("outerServer", Integer.parseInt(port.toString()),
                1024 * 64, handler, protocols);
    }

    /**
     * 暴露给内网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public NetworkServer innerServer() {
        List<ServerInfo> innerServers = serverInfoProperties.getInnerServers();
        Integer id = serverInfoProperties.getId();
        Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        ServerInfo serverInfo = optional.get();
        int port = serverInfo.getPort();
        InnerServerMessageDispatcher handler = SpringBeanFactory.getBean(InnerServerMessageDispatcher.class);
        HashSet<String> protocols = new HashSet<>();
        protocols.add("TCP");
        return new NetworkServer("innerServer", port, 1024 * 1024, handler, protocols);
    }

    /**
     * 内部客户端工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolScheduler innerClientThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "InnerClient-biz-pool";
        if (threadPoolSize != null) {
            final Object innerClient = threadPoolSize.get("innerClient");
            if (innerClient != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(innerClient.toString()));
            }
        }
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, CPU_COUNT * 2);
    }

    /**
     * 内部服务器工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolScheduler innerServerThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "InnerServer-biz-pool";
        if (threadPoolSize != null) {
            final Object innerServer = threadPoolSize.get("innerServer");
            if (innerServer != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(innerServer.toString()));
            }
        }
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, CPU_COUNT * 2);
    }

    /**
     * 外部服务器工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "hbsoo.server.outerServer", name = "enable", havingValue = "true")
    public ThreadPoolScheduler outerServerThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "OuterServer-biz-pool";
        if (threadPoolSize != null) {
            final Object outerServer = threadPoolSize.get("outerServer");
            if (outerServer != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(outerServer.toString()));
            }
        }
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, CPU_COUNT * 2);
    }

    /**
     * 延迟线程池
     */
    @Bean(destroyMethod = "shutdown")
    public DelayThreadPoolScheduler delayThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "Delay";
        if (threadPoolSize != null) {
            final Object delay = threadPoolSize.get("delay");
            if (delay != null) {
                return new DelayThreadPoolScheduler(poolName, Integer.parseInt(delay.toString()));
            }
        }
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        return new DelayThreadPoolScheduler(poolName, CPU_COUNT * 2);
    }

    /**
     * 创建外网session管理器
     */
    @Bean
    public OuterSessionManager outerSessionManager() {
        List<ServerInfo> innerServers = serverInfoProperties.getInnerServers();
        Integer id = serverInfoProperties.getId();
        Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        ServerInfo serverInfo = optional.get();
        return new OuterSessionManager(serverInfo);
    }

    @Bean(initMethod = "forwardFormDb")
    @ConditionalOnMissingBean(ForwardMessageSender.class)
    public ForwardMessageSender forwardMessageSender() {
        return new DefaultForwardMessageSender();
    }

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        Integer serverId = serverInfoProperties.getId();
        return new SnowflakeIdGenerator(1, serverId);
    }
}
