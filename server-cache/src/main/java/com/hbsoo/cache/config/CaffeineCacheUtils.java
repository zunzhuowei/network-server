package com.hbsoo.cache.config;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/17.
 */
public final class CaffeineCacheUtils {

//    public static void main(String[] args) throws ExecutionException {
//        // 定义CacheLoader，用于在缓存项不存在或刷新时加载数据
//        CacheLoader<String, String> loader = new CacheLoader<String, String>() {
//            @Override
//            public String load(String key) {
//                // 这里模拟从数据库或其他数据源加载数据
//                final long l = System.currentTimeMillis();
//                return "loadedValueForKey-" + l + "-" + key;
//            }
//        };
//
//        // 构建LoadingCache实例，并设置refreshAfterWrite策略
//        LoadingCache<String, String> cache = Caffeine.newBuilder()
//                .refreshAfterWrite(3, TimeUnit.SECONDS)
//                .expireAfterWrite(2, TimeUnit.SECONDS)
//                .build(loader);
//
//        // 将数据放入缓存（或通过get自动加载）
//        final String value = cache.get("key");
//
//        System.out.println("Initial Value: " + value);
//
//        try {
//            Thread.sleep(5000); // 等待超过refreshAfterWrite的时间，以便触发刷新逻辑
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        // 刷新操作是异步的，此处再次读取以查看是否已刷新
//        System.out.println("Value after potential refresh: " + cache.get("key"));
//
//        try {
//            Thread.sleep(5000); // 等待超过refreshAfterWrite的时间，以便触发刷新逻辑
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        // 刷新操作是异步的，此处再次读取以查看是否已刷新
//        System.out.println("Value after potential refresh2: " + cache.get("key"));
//    }

    /**
     * 构建刷新缓存,缓存写入一段时间后，过期前自动根据loader函数取刷新数据；
     * 所以，expireAfterWrite 必须大于 refreshAfterWrite;
     * 作为成员变量使用；
     */
    public static <K, V> LoadingCache<K, V> buildRefreshCache(CaffeineCache caffeineCache, CacheLoader<K, V> loader) {
        if (caffeineCache.getExpireAfterWrite() < caffeineCache.getRefreshAfterWrite()) {
            throw new RuntimeException("expireAfterWrite 必须大于 refreshAfterWrite");
        }
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(caffeineCache.getInitialCapacity())
                .maximumSize(caffeineCache.getMaximumSize())
                .expireAfterWrite(caffeineCache.getExpireAfterWrite(), TimeUnit.SECONDS)
                .refreshAfterWrite(caffeineCache.getRefreshAfterWrite(), TimeUnit.SECONDS);

        if (caffeineCache.getExpireAfterAccess() > 0) {
            caffeine.expireAfterAccess(caffeineCache.getExpireAfterAccess(), TimeUnit.SECONDS);
        }
        if (caffeineCache.isUseWeakKeys()) {
            caffeine.weakKeys();
        }
        return caffeine.build(loader);
    }

}
