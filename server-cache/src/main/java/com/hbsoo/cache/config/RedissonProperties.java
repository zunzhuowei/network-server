package com.hbsoo.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by zun.wei on 2024/6/16.
 */
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {

    private String yamlFileName;

    public String getYamlFileName() {
        return yamlFileName;
    }

    public void setYamlFileName(String yamlFileName) {
        this.yamlFileName = yamlFileName;
    }
}
