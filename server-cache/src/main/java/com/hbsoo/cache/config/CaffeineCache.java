package com.hbsoo.cache.config;

/**
 * Created by zun.wei on 2024/6/16.
 */
public class CaffeineCache {

    private int initialCapacity = 10;
    private int maximumSize = 100;
    private int expireAfterWrite = 60;
    private int expireAfterAccess;
    private int refreshAfterWrite;
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
