package com.hbsoo.cache.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zun.wei on 2024/6/16.
 */
@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfiguration {


    @Bean
    public RedissonClient redissonClient(RedissonProperties redissonProperties) throws IOException {
        String yamlFileName = redissonProperties.getYamlFileName();
        ClassPathResource classPathResource = new ClassPathResource(yamlFileName);
        InputStream inputStream = classPathResource.getInputStream();
        Config config = Config.fromYAML(inputStream);
        return Redisson.create(config);
    }

}
