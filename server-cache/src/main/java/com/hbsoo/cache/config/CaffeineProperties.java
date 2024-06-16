package com.hbsoo.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zun.wei on 2024/6/16.
 */
@ConfigurationProperties(prefix = "caffeine")
public class CaffeineProperties {

    private CaffeineCache defaultCache = new CaffeineCache();

    private Map<String, CaffeineCache> customCaches = new HashMap<>();

    public CaffeineCache getDefaultCache() {
        return defaultCache;
    }

    public void setDefaultCache(CaffeineCache defaultCache) {
        this.defaultCache = defaultCache;
    }

    public Map<String, CaffeineCache> getCustomCaches() {
        return customCaches;
    }

    public void setCustomCaches(Map<String, CaffeineCache> customCaches) {
        this.customCaches = customCaches;
    }
}
