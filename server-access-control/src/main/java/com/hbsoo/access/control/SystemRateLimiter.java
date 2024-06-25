package com.hbsoo.access.control;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by zun.wei on 2024/6/25.
 */
public final class SystemRateLimiter {

    private final RateLimiter rateLimiter; // 每秒不超过X个请求
    private final LoadingCache<Long, RateLimiter> userRateLimiters;

    public SystemRateLimiter(int globalRateSize, int userRateSize) {
        this.rateLimiter = RateLimiter.create(globalRateSize);
        this.userRateLimiters =
                CacheBuilder.newBuilder()
                        .expireAfterWrite(3, TimeUnit.SECONDS) // 用户限流器三秒钟后过期
                        .build(new CacheLoader<Long, RateLimiter>() {
                            @Override
                            public RateLimiter load(Long userId) throws Exception {
                                return RateLimiter.create(userRateSize);
                            }
                        });
    }


    public boolean tryGlobalAcquire() {
        return rateLimiter.tryAcquire(); // 尝试获取令牌，如果当前请求超过限流速率则返回false
    }

    public boolean tryUserAcquire(Long userId) throws ExecutionException {
        return userRateLimiters.get(userId).tryAcquire();
    }
}
