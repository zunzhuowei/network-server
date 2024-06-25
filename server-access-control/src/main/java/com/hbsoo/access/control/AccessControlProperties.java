package com.hbsoo.access.control;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Created by zun.wei on 2024/6/16.
 */
@ConfigurationProperties(prefix = "hbsoo.server.access")
public final class AccessControlProperties {

    /**
     * 全局限流,每秒访问次数
     */
    private int globalRateSize = 1000;

    /**
     * 用户限流,每秒访问次数
     */
    private int userRateSize = 10;

    /**
     * 屏蔽ip
     */
    private List<String> blockIpList;

    /**
     * 白名单
     */
    private List<String> whiteIpList;


    public int getGlobalRateSize() {
        return globalRateSize;
    }

    public void setGlobalRateSize(int globalRateSize) {
        this.globalRateSize = globalRateSize;
    }

    public int getUserRateSize() {
        return userRateSize;
    }

    public void setUserRateSize(int userRateSize) {
        this.userRateSize = userRateSize;
    }

    public List<String> getBlockIpList() {
        return blockIpList;
    }

    public void setBlockIpList(List<String> blockIpList) {
        this.blockIpList = blockIpList;
    }

    public List<String> getWhiteIpList() {
        return whiteIpList;
    }

    public void setWhiteIpList(List<String> whiteIpList) {
        this.whiteIpList = whiteIpList;
    }
}
