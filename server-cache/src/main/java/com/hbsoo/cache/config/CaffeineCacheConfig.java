package com.hbsoo.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching // 启用缓存管理
@EnableConfigurationProperties(CaffeineProperties.class)
public class CaffeineCacheConfig {

    @Bean("cacheManager")
    @Primary
    public CacheManager cacheManager(CaffeineProperties caffeineProperties) {
        CaffeineCache defaultCache = caffeineProperties.getDefaultCache();

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 设置Caffeine配置
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(defaultCache.getInitialCapacity()) // 设置初始容量
                .maximumSize(defaultCache.getMaximumSize()) // 设置缓存的最大容量
                .expireAfterWrite(defaultCache.getExpireAfterWrite(), TimeUnit.SECONDS) // 设置缓存在写入后多久过期
                //.expireAfterAccess(30, TimeUnit.SECONDS) // 设置缓存在访问后多久过期
                //.weakKeys() // 使用弱键，当键被垃圾回收时，键和值都会从缓存中移除
                //.removalListener((key, value, cause) -> {
                //    // 移除缓存时的回调函数
                //    System.out.println("Cache removed: " + key);
                //})
                //.refreshAfterWrite(10, TimeUnit.SECONDS) // 设置缓存在写入后多久刷新
                //.executor(Runnable::run) // 设置异步刷新的线程池
                .recordStats(); // 记录统计信息，用于监控
        if (defaultCache.getExpireAfterAccess() > 0) {
            caffeine.expireAfterAccess(defaultCache.getExpireAfterAccess(), TimeUnit.SECONDS);
        }
        if (defaultCache.isUseWeakKeys()) {
            caffeine.weakKeys();
        }
        //if (defaultCache.getRefreshAfterWrite() > 0) {
        //    caffeine.refreshAfterWrite(defaultCache.getRefreshAfterWrite(), TimeUnit.SECONDS);
        //}
        cacheManager.setCaffeine(caffeine);
        cacheManager.setAllowNullValues(true);
        // 可以为特定缓存设置不同的过期策略
        final Map<String, CaffeineCache> customCaches = caffeineProperties.getCustomCaches();
        customCaches.forEach((name, cache) -> {
            cacheManager.registerCustomCache(name, buildCustomCache(cache));
        });
        //cacheManager.registerCustomCache("customCacheName", buildCustomCache());

        return cacheManager;
    }

    // 自定义某个缓存的配置
    private Cache<Object, Object> buildCustomCache(CaffeineCache caffeineCache) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(caffeineCache.getInitialCapacity()) // 设置初始容量
                .maximumSize(caffeineCache.getMaximumSize()) // 设置缓存的最大容量
                .expireAfterWrite(caffeineCache.getExpireAfterWrite(), TimeUnit.SECONDS) // 设置缓存在写入后多久过期
                .recordStats(); // 记录统计信息，用于监控
        if (caffeineCache.getExpireAfterAccess() > 0) {
            caffeine.expireAfterAccess(caffeineCache.getExpireAfterAccess(), TimeUnit.SECONDS);
        }
        if (caffeineCache.isUseWeakKeys()) {
            caffeine.weakKeys();
        }
        //refreshAfterWrite requires a LoadingCache
        //if (caffeineCache.getRefreshAfterWrite() > 0) {
        //    caffeine.refreshAfterWrite(caffeineCache.getRefreshAfterWrite(), TimeUnit.SECONDS);
        //}
        return caffeine.build();
    }

    /*
    //refreshAfterWrite requires a LoadingCache
    return caffeine.build(new CacheLoader<Key, Value>() {
    @Override
    public Value load(Key key) throws Exception {
        // 在这里加载数据
    }
    });
     */
}

