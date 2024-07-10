package com.hbsoo.server.config;

import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.client.DefaultInsideTcpClientConnectListener;
import com.hbsoo.server.message.client.InsideClientMessageDispatcher;
import com.hbsoo.server.message.sender.DefaultForwardMessageSender;
import com.hbsoo.server.message.sender.ForwardMessageSender;
import com.hbsoo.server.message.server.InsideServerMessageDispatcher;
import com.hbsoo.server.message.server.OutsideServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
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
        InsideClientMessageDispatcher.class,
        InsideServerMessageDispatcher.class,
        OutsideServerMessageDispatcher.class,
        DefaultInsideTcpClientConnectListener.class,
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
    @ConditionalOnProperty(prefix = "hbsoo.server.outsideServer", name = "enable", havingValue = "true")
    public NetworkServer outsideServer() {
        Map<String, Object> outsideServer = serverInfoProperties.getOutsideServer();
        Object port = outsideServer.get("port");
        String protocolsStr = Objects.isNull(outsideServer.get("protocol"))
                ? "TCP,UDP,WEBSOCKET,HTTP" : outsideServer.get("protocol").toString();
        HashSet<String> protocols = new HashSet<>(Arrays.asList(protocolsStr.split(",")));
        OutsideServerMessageDispatcher handler = SpringBeanFactory.getBean(OutsideServerMessageDispatcher.class);
        return new NetworkServer("OS", Integer.parseInt(port.toString()),
                1024 * 64, handler, protocols);
    }

    /**
     * 暴露给内网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public NetworkServer insideServer() {
        List<ServerInfo> insideServers = serverInfoProperties.getInsideServers();
        Integer id = serverInfoProperties.getId();
        Optional<ServerInfo> optional = insideServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        ServerInfo serverInfo = optional.get();
        int port = serverInfo.getPort();
        InsideServerMessageDispatcher handler = SpringBeanFactory.getBean(InsideServerMessageDispatcher.class);
        HashSet<String> protocols = new HashSet<>();
        protocols.add("TCP");
        return new NetworkServer("IS", port, 1024 * 1024, handler, protocols);
    }

    /**
     * 内部客户端工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolScheduler insideClientThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "IC-biz-pool";
        if (threadPoolSize != null) {
            final Object insideClient = threadPoolSize.get("insideClient");
            if (insideClient != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(insideClient.toString()));
            }
        }
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, CPU_COUNT * 2);
    }

    /**
     * 内部服务器工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolScheduler insideServerThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "IS-biz-pool";
        if (threadPoolSize != null) {
            final Object insideServer = threadPoolSize.get("insideServer");
            if (insideServer != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(insideServer.toString()));
            }
        }
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, processors * 2);
    }

    /**
     * 外部服务器工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    //@ConditionalOnProperty(prefix = "hbsoo.server.outerServer", name = "enable", havingValue = "true")
    public ThreadPoolScheduler outsideServerThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "OS-biz-pool";
        if (threadPoolSize != null) {
            final Object outsideServer = threadPoolSize.get("outsideServer");
            if (outsideServer != null) {
                return new ThreadPoolScheduler(poolName, Integer.parseInt(outsideServer.toString()));
            }
        }
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolScheduler(poolName, processors * 2);
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
        int processors = Runtime.getRuntime().availableProcessors();
        return new DelayThreadPoolScheduler(poolName, processors * 2);
    }

    /**
     * 创建外网session管理器
     */
    @Bean
    public OutsideUserSessionManager outsideSessionManager() {
        List<ServerInfo> insideServers = serverInfoProperties.getInsideServers();
        Integer id = serverInfoProperties.getId();
        Optional<ServerInfo> optional = insideServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        ServerInfo serverInfo = optional.get();
        return new OutsideUserSessionManager(serverInfo);
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
