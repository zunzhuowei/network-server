package com.hbsoo.server.config;

import com.hbsoo.server.NetworkServer;
import com.hbsoo.server.action.InnerClientHeartbeatAction;
import com.hbsoo.server.action.InnerClientLoginAction;
import com.hbsoo.server.action.InnerServerHeartbeatAction;
import com.hbsoo.server.action.InnerServerLoginAction;
import com.hbsoo.server.client.TcpClientRegister;
import com.hbsoo.server.message.client.InnerTcpClientMessageDispatcher;
import com.hbsoo.server.message.server.*;
import com.hbsoo.server.utils.DelayThreadPoolScheduler;
import com.hbsoo.server.utils.SpringBeanFactory;
import com.hbsoo.server.utils.ThreadPoolScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by zun.wei on 2020/4/29.
 */
//@ComponentScan(basePackages = {"com.hbsoo.server.action"})
@Import({TcpClientRegister.class, InnerServerHeartbeatAction.class, InnerClientHeartbeatAction.class})
@Configuration
@EnableConfigurationProperties(ServerInfoProperties.class)
public class NetworkServerAutoConfiguration {

    @Autowired
    private ServerInfoProperties serverInfoProperties;

    @Bean
    public SpringBeanFactory springBeanFactory() {
        return new SpringBeanFactory();
    }

    @Bean
    public InnerServerLoginAction innerServerLoginAction() {
        return new InnerServerLoginAction();
    }

    @Bean
    public InnerClientLoginAction innerClientLoginAction() {
        return new InnerClientLoginAction();
    }

    @Bean
    public InnerTcpClientMessageDispatcher innerTcpClientMessageDispatcher() {
        return new InnerTcpClientMessageDispatcher();
    }

    @Bean
    public InnerTcpServerMessageDispatcher innerTcpServerMessageDispatcher() {
        return new InnerTcpServerMessageDispatcher();
    }

    @Bean
    public InnerWebsocketServerMessageDispatcher innerWebsocketServerMessageDispatcher() {
        return new InnerWebsocketServerMessageDispatcher();
    }

    @Bean
    public InnerHttpServerMessageDispatcher innerHttpServerMessageDispatcher() {
        return new InnerHttpServerMessageDispatcher();
    }

    @Bean
    public InnerUdpServerMessageDispatcher innerUdpServerMessageDispatcher() {
        return new InnerUdpServerMessageDispatcher();
    }

    @Bean
    public OuterHttpServerMessageDispatcher outerHttpServerMessageDispatcher() {
        return new OuterHttpServerMessageDispatcher();
    }

    @Bean
    public OuterTcpServerMessageDispatcher outerTcpServerMessageDispatcher() {
        return new OuterTcpServerMessageDispatcher();
    }

    @Bean
    public OuterUdpServerMessageDispatcher outerUdpServerMessageDispatcher() {
        return new OuterUdpServerMessageDispatcher();
    }

    @Bean
    public OuterWebsocketServerMessageDispatcher outerWebsocketServerMessageDispatcher() {
        return new OuterWebsocketServerMessageDispatcher();
    }
//    @Bean
//    public HttpIndexAction httpIndexAction() {
//        return new HttpIndexAction();
//    }

    /**
     * 暴露给外网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "hbsoo.server.outerServer", name = "enable", havingValue = "true")
    public NetworkServer outerServer() {
        final Map<String, Object> outerServer = serverInfoProperties.getOuterServer();
        final Object port = outerServer.get("port");
        ServerMessageHandler[] handlers = new ServerMessageHandler[]{
                springBeanFactory().getBean("outerHttpServerMessageDispatcher", OuterHttpServerMessageDispatcher.class),
                springBeanFactory().getBean("outerTcpServerMessageDispatcher", OuterTcpServerMessageDispatcher.class),
                springBeanFactory().getBean("outerUdpServerMessageDispatcher", OuterUdpServerMessageDispatcher.class),
                springBeanFactory().getBean("outerWebsocketServerMessageDispatcher", OuterWebsocketServerMessageDispatcher.class)
        };
        return new NetworkServer("outerServer", Integer.parseInt(port.toString()), handlers);
    }

    /**
     * 暴露给内网的端口服务器
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public NetworkServer innerServer() {
        final List<ServerInfo> innerServers = serverInfoProperties.getInnerServers();
        final Integer id = serverInfoProperties.getId();
        final Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        final ServerInfo serverInfo = optional.get();
        final int port = serverInfo.getPort();
        ServerMessageHandler[] handlers = new ServerMessageHandler[]{
                springBeanFactory().getBean("innerTcpServerMessageDispatcher", InnerTcpServerMessageDispatcher.class),
                springBeanFactory().getBean("innerWebsocketServerMessageDispatcher", InnerWebsocketServerMessageDispatcher.class),
                springBeanFactory().getBean("innerHttpServerMessageDispatcher", InnerHttpServerMessageDispatcher.class),
                springBeanFactory().getBean("innerUdpServerMessageDispatcher", InnerUdpServerMessageDispatcher.class)
        };
        return new NetworkServer("innerServer", port, handlers);
    }

    /**
     * 内部客户端工作线程池
     */
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolScheduler innerClientThreadPoolScheduler() {
        final Map<String, Object> threadPoolSize = serverInfoProperties.getThreadPoolSize();
        String poolName = "InnerClient";
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
        String poolName = "InnerServer";
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
        String poolName = "OuterServer";
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

}
