package com.hbsoo.server.client;

import com.hbsoo.server.NowServer;
import com.hbsoo.server.config.ServerInfo;
import com.hbsoo.server.session.ServerType;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 将内部服务链接用的客户端 注册到spring容器中
 * Created by zun.wei on 2024/6/9.
 */
public final class TcpClientRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        List<ServerInfo> innerServers = new ArrayList<>();
        // 解析配置文件中的内网服务器配置
        MutablePropertySources sources = ((AbstractEnvironment) environment).getPropertySources();
        for (PropertySource<?> source : sources) {
            if (source instanceof EnumerablePropertySource) {
                final String[] names = ((EnumerablePropertySource) source).getPropertyNames();
                List<String> collect = Arrays.stream(names).filter(name -> name.startsWith("hbsoo.server.innerServers")).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < collect.size(); i++) {
                    String hostKey = "hbsoo.server.innerServers[" + i + "].host";
                    String portKey = "hbsoo.server.innerServers[" + i + "].port";
                    String typeKey = "hbsoo.server.innerServers[" + i + "].type";
                    String idKey = "hbsoo.server.innerServers[" + i + "].id";
                    if (StringUtils.hasText(environment.getProperty(hostKey))) {
                        if (StringUtils.hasText(environment.getProperty(portKey))) {
                            if (StringUtils.hasText(environment.getProperty(typeKey))) {
                                if (StringUtils.hasText(environment.getProperty(idKey))) {
                                    ServerInfo serverInfo = new ServerInfo();
                                    serverInfo.setHost(environment.getProperty(hostKey));
                                    serverInfo.setPort(Integer.parseInt(environment.getProperty(portKey)));
                                    serverInfo.setType(ServerType.valueOf(environment.getProperty(typeKey)));
                                    serverInfo.setId(Integer.parseInt(environment.getProperty(idKey)));
                                    innerServers.add(serverInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
        final String serverIdStr = environment.getProperty("hbsoo.server.id");
        Integer id = Integer.parseInt(serverIdStr);//当前服务器id
        Optional<ServerInfo> optional = innerServers.stream().filter(e -> e.getId().equals(id)).findFirst();
        ServerInfo fromServerInfo = optional.get();//当前服务器信息

        //填充当前服务器信息
        NowServer.setServerInfo(fromServerInfo);

        // 注册内网客户端
        for (ServerInfo innerServer : innerServers) {
            if (innerServer.getId().equals(id)) {
                continue;
            }
            //每个服务器使用五个客户端链接
            for (int i = 0; i < 5; i++) {
                AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(TcpClient.class).getBeanDefinition();
                beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                beanDefinition.setScope("singleton");
                beanDefinition.setInitMethodName("start");
                beanDefinition.setDestroyMethodName("stop");
                ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
                constructorArgumentValues.addIndexedArgumentValue(0, fromServerInfo);
                constructorArgumentValues.addIndexedArgumentValue(1, innerServer);
                constructorArgumentValues.addIndexedArgumentValue(2, 3);
                constructorArgumentValues.addIndexedArgumentValue(3, i);
                beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
                String beanName = "innerClient:" + fromServerInfo.getId() + " to " + innerServer.getId() + "#" + i;
                beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


}