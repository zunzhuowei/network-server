package com.hbsoo.cache.config;

/**
 * Created by zun.wei on 2024/6/16.
 */
public class CaffeineCache {

    //初始化缓存数量
    private int initialCapacity = 10;
    //最大缓存数量
    private int maximumSize = 100;
    //写入多久之后过期（秒）
    private int expireAfterWrite = 60;
    //访问多久之后过期（秒）
    private int expireAfterAccess;
    //写入之后多久取刷新缓存（秒）
    private int refreshAfterWrite;
    //是否使用弱引用
    private boolean useWeakKeys = false;


    public int getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public int getExpireAfterAccess() {
        return expireAfterAccess;
    }

    public void setExpireAfterAccess(int expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    public int getExpireAfterWrite() {
        return expireAfterWrite;
    }

    public void setExpireAfterWrite(int expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    public int getRefreshAfterWrite() {
        return refreshAfterWrite;
    }

    public void setRefreshAfterWrite(int refreshAfterWrite) {
        this.refreshAfterWrite = refreshAfterWrite;
    }

    public boolean isUseWeakKeys() {
        return useWeakKeys;
    }

    public void setUseWeakKeys(boolean useWeakKeys) {
        this.useWeakKeys = useWeakKeys;
    }
}
